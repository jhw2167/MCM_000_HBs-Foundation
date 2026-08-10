package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.mojang.datafixers.util.Either;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ChunkExplorerManager
 *
 * One instance per loaded level. Every 1200 ticks it collects all online
 * players in each level, merges their exploration spirals into a single
 * ordered Set<ChunkPos>, and queues those not yet present in
 * ManagedChunk.INITIALIZED_CHUNKS. Each server tick one chunk is
 * force-loaded (if not already initialized) and immediately released so
 * that BiomeManager / StructureManager onChunkLoad listeners can harvest it.
 */
public class ChunkExplorerManager {

    public static final String CLASS_ID = "037";


    public static final int SCAN_RADIUS_CHUNKS = 64;//625;   // 10,000 blocks / 16, RANGE
    public static final int MIN_HOLD_TICKS = 2;     // ticks a probe stays loaded
    public static final int MAX_HOLD_TICKS = 40;     // ticks a probe stays loaded
    public static final int CHUNK_EXPLORER_MAX = 50;  // safety cap to prevent OOM if something goes wrong with the queue rebuild
    public static final int SCAN_RADIUS_SQUARED = SCAN_RADIUS_CHUNKS*SCAN_RADIUS_CHUNKS;//625;   // 10,000 blocks / 16, RANGE


    private static final String TICKET_PREFIX = "chunk_explorer_";
    private static final Map<Level, ChunkExplorerManager> managers = new HashMap<>();
    private static GeneralConfig GENERAL_CONFIG;

    private final Level level;
    private final ManagedChunkUtility util;

    // Ordered queue of positions still needing exploration this pass
    //Unique queue that maps the chunk distance between some player to its chunk position
    private final Deque<ChunkPos> chunkExploreQueue = new LinkedList<>();
    private final Map<Player, ChunkGenerationOrderHandler> chunkGenerators = new HashMap<>();
    private final Set<ChunkPos> requestedChunks = new HashSet<>();

    // Single probe currently held open, and the tick it was loaded
    private ChunkPos heldChunk = null;
    private long     heldSince = Long.MIN_VALUE;
    CompletableFuture<ChunkResult<ChunkAccess>>     heldChunkFuture = null;

    private ChunkExplorerManager(Level level) {
        this.level = level;
        this.util = ManagedChunkUtility.getInstance(level);
    }


    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(ChunkExplorerManager::onServerStart);
        reg.registerOnLevelLoad(ChunkExplorerManager::onLevelLoad);
        reg.registerOnLevelUnload(ChunkExplorerManager::onLevelUnload);
        reg.registerOnServerTick(TickType.ON_SINGLE_TICK, ChunkExplorerManager::onSingleTick);
        reg.registerOnServerTick(TickType.ON_120_TICKS,  ChunkExplorerManager::on1200Ticks);
    }

    //** GETTERS

    public int queueSize() {
        return chunkExploreQueue.size();
    }

    //** PER-TICK LOGIC

    private void onTick(ServerTickEvent event)
    {
        long now = GENERAL_CONFIG.getTotalTickCount();

        /*
        if (heldChunk != null && (now - heldSince) > MIN_HOLD_TICKS) {
            if(util.isChunkInitialized(heldChunk) || now - heldSince > MAX_HOLD_TICKS) {
                HBUtil.ChunkUtil.unforceLoadChunk((ServerLevel) level, heldChunk, ticketId(heldChunk), 0);
                heldChunk = null;
            }
        }
         */

        //aaa
        if (heldChunk == null) {
            ChunkPos next = pollNextUninitialized();
            if (next != null) {
                heldChunk = next;
                heldSince = now;
                HBUtil.ChunkUtil.softLoadChunk((ServerLevel) level, next ).thenAccept( o -> heldChunk = null);
                requestedChunks.add(next);
                //HBUtil.ChunkUtil.forceLoadChunk((ServerLevel) level, next, ticketId(next), 0);
            }
        }
    }

    private ChunkPos pollNextUninitialized()
    {
        while (!chunkExploreQueue.isEmpty())
        {
            ChunkPos candidate = chunkExploreQueue.poll();
            if (candidate == null) return  null;
            if (util.isChunkInitialized(candidate)) continue;
            if (util.isLoaded(candidate)) continue;

            return candidate;
        }

        return null;
    }

    private static int SKIP_CHUNKS = 8;
    //** QUEUE REBUILD (every 1200 ticks)
    private void rebuildQueue()
    {
        if (chunkGenerators.isEmpty()) return;

        List<Player> players = new ArrayList<>(chunkGenerators.keySet());
        for (Player player : players)
        {
            ChunkGenerationOrderHandler handler = chunkGenerators.get(player);
            //ChunkPos playerChunk = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
            final int maxPerPlayer = CHUNK_EXPLORER_MAX / chunkGenerators.size();
            int count = 0;
            while(true)
            {
                if(chunkExploreQueue.size() > CHUNK_EXPLORER_MAX) break;

                ChunkPos next = handler.getNextUnInitSpiralChunk(util, SKIP_CHUNKS);
                if(next == null) break;
                if(handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS)) break;

                chunkExploreQueue.addLast(next);
                if(count++ > maxPerPlayer) break;
            }
        }

    }

    //** HELPERS

    private static String ticketId(ChunkPos pos) {
        //return TICKET_PREFIX + pos.x + "_" + pos.z;
        return TICKET_PREFIX;
    }

    private record ScoredChunk(ChunkPos pos, long distSq) {}

    //** STATICS

    public static ChunkExplorerManager get(Level level) {
        if (GENERAL_CONFIG.isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!managers.containsKey(level))
            initLevel(level);
        return managers.get(level);
    }

    private static ChunkExplorerManager initLevel(Level level) {
        if (!managers.containsKey(level)) {
            managers.put(level, new ChunkExplorerManager(level));
        }
        return managers.get(level);
    }

    //** EVENTS

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
        GENERAL_CONFIG = GeneralConfig.getInstance();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        ChunkExplorerManager.initLevel((Level) event.getLevel());
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ChunkExplorerManager manager = managers.remove(event.getLevel());
        if (manager != null && manager.heldChunk != null) {
            HBUtil.ChunkUtil.unforceLoadChunk(
                (ServerLevel) event.getLevel(),
                manager.heldChunk,
                ticketId(manager.heldChunk),
                2
            );
        }
    }

    private static void onSingleTick(ServerTickEvent event) {
        for (ChunkExplorerManager manager : managers.values()) {
            manager.onTick(event);
        }
    }

    /**
     * Find new chunks nearby players to explore
     * @param event
     */
    private static void on1200Ticks(ServerTickEvent event)
    {
        if (GENERAL_CONFIG.getServer() == null) return;

        Map<Level, List<Player>> playersByLevel = new HashMap<>();
        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            playersByLevel
                .computeIfAbsent((ServerLevel) player.level(), k -> new ArrayList<>())
                .add(player);
        }

        for (Map.Entry<Level, ChunkExplorerManager> entry : managers.entrySet()) {
            Level level = entry.getKey();
            if(!playersByLevel.containsKey(level)) continue;
            ChunkExplorerManager manager = entry.getValue();
            manager.generateDistantChunks(playersByLevel.get(level));
        }
    }

    private static final int PLAYER_RENDER_DIST_SQ = 16*16;
    private static final double SKIP_RATIO = SCAN_RADIUS_SQUARED/PLAYER_RENDER_DIST_SQ;
    private void generateDistantChunks(List<Player> players) {

        for(Player player : players) {
            if(!chunkGenerators.containsKey(player)) {
                ChunkPos p = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
                chunkGenerators.put(player, new ChunkGenerationOrderHandler(p, 0, SCAN_RADIUS_CHUNKS) );
            }
            ChunkGenerationOrderHandler handler = chunkGenerators.get(player);
            if(handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS)) {
                ChunkPos p = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
                double skipRatio = (double) PLAYER_RENDER_DIST_SQ / handler.getSkippedChunks()*SKIP_CHUNKS;
                int radialOffset = Math.max(0, (int) skipRatio );
                chunkGenerators.put(player, new ChunkGenerationOrderHandler(p, radialOffset, SCAN_RADIUS_CHUNKS) );
            }
        }

        this.rebuildQueue();
    }

    //** INNER CLASS

    /**
     * Generates ChunkPos values in an expanding square spiral.
     * Translated directly from the provided implementation, with one fix
     * applied: the direction-change condition now correctly handles the
     * transition from the very first step so that chunk (1, 0) is not skipped.
     */
    private static class ChunkGenerationOrderHandler {

        private static final int[] UP    = {0,  1};
        private static final int[] RIGHT = {1,  0};
        private static final int[] DOWN  = {0, -1};
        private static final int[] LEFT  = {-1, 0};
        private static final int[][] DIRECTIONS = {UP, RIGHT, DOWN, LEFT};
        private static final Random RANDOM = new Random();

        private ChunkPos currentPos;
        private final ChunkPos startPos;
        private int total;
        private int count;
        private int dirCount;
        private int radialOffset;
        private int skippedChunks;
        private int radius;
        private int[] dir;

        public ChunkGenerationOrderHandler(ChunkPos start) {
            this.currentPos = (start == null) ? new ChunkPos(0, 0) : start;
            this.startPos   = this.currentPos;
            this.total    = 0;
            this.count    = 1;
            this.dirCount = 0;
            this.skippedChunks = 0;
            this.dir      = UP;
        }

        public ChunkGenerationOrderHandler(ChunkPos start, int radialOffset, int radius) {
            this(start);
            //offset currentPos to right, left, up or down, randomly
            int[] offsetDir = DIRECTIONS[RANDOM.nextInt(DIRECTIONS.length)];
            this.currentPos = HBUtil.ChunkUtil.posAdd(
                this.currentPos,
                new ChunkPos(offsetDir[0]*radialOffset, offsetDir[1]*radialOffset)
            );
             this.radialOffset = radialOffset;
            this.radius = radius;
        }

        public ChunkPos getNextSpiralChunk() {
            if (total == 0) {
                total++;
                return currentPos;
            }

            if (dirCount == count) {
                dir      = getNextDirection();
                dirCount = 0;
                // Increment the step-count after completing UP or DOWN legs
                if (dir == UP || dir == DOWN) {
                    count++;
                }
            }

            currentPos = HBUtil.ChunkUtil.posAdd(currentPos, dir);
            total++;
            dirCount++;

            return currentPos;
        }

        public ChunkPos getNextUnInitSpiralChunk(ManagedChunkUtility util, int skipChunks)
        {
            if (total == 0) {
                total++; return currentPos;
            }

            int totalStart = total;
            int nextPosX = currentPos.x() + dir[0]*skipChunks;
            int nextPosZ = currentPos.z() + dir[1]*skipChunks;
            while(util.isChunkInitialized(nextPosX, nextPosZ))
            {
                //check if nextPosX is outside of radius from startPos
                if(nextPosX > startPos.x()) {
                    if(nextPosX - startPos.x() > radius) break;
                } else {
                    if(startPos.x() - nextPosX > radius) break;
                }

                if(nextPosZ > startPos.z()) {
                    if(nextPosZ - startPos.z() > radius) break;
                } else {
                    if(startPos.z() - nextPosZ > radius) break;
                }


                if (dirCount == count) {
                    dir      = getNextDirection();
                    dirCount = 0;
                    // Increment the step-count after completing UP or DOWN legs
                    if (dir == UP || dir == DOWN) {
                        count++;
                    }
                }

                nextPosX += (dir[0]*skipChunks);
                nextPosZ += (dir[1]*skipChunks);
                total++;
                dirCount++;
            }

            skippedChunks += (total - totalStart);
            currentPos = new ChunkPos(nextPosX, nextPosZ);
            return currentPos;
        }

        private int[] getNextDirection() {
            int index = Arrays.asList(DIRECTIONS).indexOf(dir);
            return DIRECTIONS[(index + 1) % DIRECTIONS.length];
        }

        /**
         * Returns true once the spiral has walked beyond the given chunk radius
         * (measured in chunk-steps from the origin in either axis).
         */
        public boolean testScanRadiusExceeded(int radiusChunks) {
            return HBUtil.ChunkUtil.chunkDistSquared(startPos, currentPos) > radiusChunks*radiusChunks;
        }

        public int getRadialOffset() {
            return radialOffset;
        }

        public int getSkippedChunks() {
            return skippedChunks;
        }
    }
}
