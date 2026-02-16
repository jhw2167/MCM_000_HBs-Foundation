package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

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


    public static final int SCAN_RADIUS_CHUNKS = 100;//625;   // 10,000 blocks / 16, RANGE
    public static final int SCAN_RADIUS_SQUARED = SCAN_RADIUS_CHUNKS*SCAN_RADIUS_CHUNKS;//625;   // 10,000 blocks / 16, RANGE
    public static final int HOLD_TICKS         = 4;     // ticks a probe stays loaded
    public static final int CHUNK_EXPLORER_MAX = 100;  // safety cap to prevent OOM if something goes wrong with the queue rebuild


    private static final String TICKET_PREFIX = "chunk_explorer_";
    private static final Map<Level, ChunkExplorerManager> managers = new HashMap<>();
    private static GeneralConfig GENERAL_CONFIG;

    private final Level level;
    private final ManagedChunkUtility util;

    // Ordered queue of positions still needing exploration this pass
    //Unique queue that maps the chunk distance between some player to its chunk position
    private final IntObjectMap<ChunkPos> chunkExploreDistanceQueue = new IntObjectHashMap<>();

    // Single probe currently held open, and the tick it was loaded
    private ChunkPos heldChunk = null;
    private long     heldSince = Long.MIN_VALUE;
    private int nextChunkDist = 1;

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
        return chunkExploreDistanceQueue.size();
    }

    //** PER-TICK LOGIC

    private void onTick(ServerTickEvent event)
    {
        long now = GENERAL_CONFIG.getTotalTickCount();

        if (heldChunk != null && (now - heldSince) > HOLD_TICKS) {
            HBUtil.ChunkUtil.unforceLoadChunk((ServerLevel) level, heldChunk, ticketId(heldChunk));
            heldChunk = null;
        }

        if (heldChunk == null) {
            ChunkPos next = pollNextUninitialized();
            if (next != null) {
                HBUtil.ChunkUtil.forceLoadChunk((ServerLevel) level, next, ticketId(next));
                heldChunk = next;
                heldSince = now;
            }
        }
    }

    private ChunkPos pollNextUninitialized()
    {
        while (!chunkExploreDistanceQueue.isEmpty())
        {
            while(!chunkExploreDistanceQueue.containsKey(++nextChunkDist) &&
            nextChunkDist < SCAN_RADIUS_SQUARED ) {
                //count
            }

            if(nextChunkDist > SCAN_RADIUS_SQUARED) {
                nextChunkDist = 1;
                return null;
            }

            ChunkPos candidate = chunkExploreDistanceQueue.remove(nextChunkDist);
            if(candidate == null) {}
            else if (util.isChunkInitialized(candidate)) {}
            else if(util.isLoaded(candidate)) {}
            else return candidate;

        }
        return null;
    }

    private static int SKIP_CHUNKS = 8;
    //** QUEUE REBUILD (every 1200 ticks)
    private void rebuildQueue(List<BlockPos> playerPositions)
    {
        if (playerPositions.isEmpty()) return;

        chunkExploreDistanceQueue.clear();
        int count = 0;
        for (BlockPos origin : playerPositions)
        {
            ChunkPos playerChunk = new ChunkPos(origin);
            ChunkGenerationOrderHandler handler = new ChunkGenerationOrderHandler(playerChunk);
            while (!handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS))
            {
                ChunkPos next = handler.getNextUnInitSpiralChunk(util);
                if(next == null) continue;
                if(count++ % SKIP_CHUNKS != 0) continue;

                int chunkDist = HBUtil.ChunkUtil.chunkDistSquared(playerChunk, next);
                if (chunkDist > SCAN_RADIUS_SQUARED) break;
                if(chunkExploreDistanceQueue.size() > CHUNK_EXPLORER_MAX) break;

                chunkExploreDistanceQueue.put(chunkDist, next);
            }
        }
         nextChunkDist = 1;
    }

    //** HELPERS

    private static String ticketId(ChunkPos pos) {
        return TICKET_PREFIX + pos.x + "_" + pos.z;
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
                ticketId(manager.heldChunk)
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
    private static void on1200Ticks(ServerTickEvent event) {
        if (GENERAL_CONFIG.getServer() == null) return;

        Map<Level, List<BlockPos>> playersByLevel = new HashMap<>();
        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            playersByLevel
                .computeIfAbsent(player.serverLevel(), k -> new ArrayList<>())
                .add(player.blockPosition());
        }

        for (Map.Entry<Level, ChunkExplorerManager> entry : managers.entrySet()) {
            if(!playersByLevel.keySet().contains(entry.getKey())) continue;

            entry.getValue().rebuildQueue(playersByLevel.get(entry.getKey()));
        }
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

        private ChunkPos currentPos;
        private int total;
        private int count;
        private int dirCount;
        private int[] dir;

        public ChunkGenerationOrderHandler(ChunkPos start) {
            this.currentPos = (start == null) ? new ChunkPos(0, 0) : start;
            this.total    = 0;
            this.count    = 1;
            this.dirCount = 0;
            this.dir      = UP;
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

        public ChunkPos getNextUnInitSpiralChunk(ManagedChunkUtility util)
        {
            if (total == 0) {
                total++; return currentPos;
            }

            int nextPosX = currentPos.x + dir[0];
            int nextPosZ = currentPos.z + dir[1];
            while(util.isChunkInitialized(nextPosX, nextPosZ))
            {
                if (dirCount == count) {
                    dir      = getNextDirection();
                    dirCount = 0;
                    // Increment the step-count after completing UP or DOWN legs
                    if (dir == UP || dir == DOWN) {
                        count++;
                    }
                }

                nextPosX += dir[0];
                nextPosZ += dir[1];
                total++;
                dirCount++;
            }

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
            return Math.abs(currentPos.x) > radiusChunks ||
                Math.abs(currentPos.z) > radiusChunks;
        }
    }
}
