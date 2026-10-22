package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.holybuckets.foundation.platform.services.ChunkLoader;
import com.mojang.datafixers.util.Either;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;


import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ChunkExplorerManager
 *
 * 1. Triggers every 1200
 */
public class ChunkExplorerManager {

    public static final String CLASS_ID = "037";


    public static final int SCAN_RADIUS_CHUNKS = 1024; //Get out there and scan!
    public static final int CHUNK_EXPLORER_MAX = 100;  // safety cap to prevent OOM if something goes wrong with the queue rebuild
    public static final int SCAN_RADIUS_SQUARED = SCAN_RADIUS_CHUNKS*SCAN_RADIUS_CHUNKS;  // 10,000 chunks / 16, RANGE


    private static final String TICKET_PREFIX = "chunk_explorer_";
    private static final Map<Level, ChunkExplorerManager> managers = new HashMap<>();
    private static GeneralConfig GENERAL_CONFIG;

    private final Level level;
    private final ManagedChunkUtility util;

    // Ordered queue of positions still needing exploration this pass
    //Unique queue that maps the chunk distance between some player to its chunk position
    private final Deque<ChunkPos> chunkExploreQueue = new LinkedList<>();
    // Chunks submitted by other systems that should be generated ahead of the spiral walk
    private final Set<ChunkPos> chunkGenerationPrioritySet = new HashSet<>();
    private final Deque<ChunkPos> chunkGenerationPriorityQueue = new LinkedList<>();
    private final Map<Player, ChunkGenerationOrderHandler> chunkGenerators = new HashMap<>();
    private final Set<ChunkPos> requestedChunks = new HashSet<>();

    // Single probe currently held open, and the tick it was loaded
    private ChunkPos heldChunk = null;
    private long     heldSince = Long.MIN_VALUE;
    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>     heldChunkFuture = null;

    private ChunkExplorerManager(Level level) {
        this.level = level;
        this.util = ManagedChunkUtility.getInstance(level);
    }


    private static ChunkLoader CHUNK_LOADER;

    public static void init(EventRegistrar reg, ChunkLoader chunkLoader) {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        CHUNK_LOADER = chunkLoader;
        reg.registerOnBeforeServerStarted(ChunkExplorerManager::onServerStart);
        reg.registerOnLevelLoad(ChunkExplorerManager::onLevelLoad);
        reg.registerOnLevelUnload(ChunkExplorerManager::onLevelUnload);
        reg.registerOnServerStopped(ChunkExplorerManager::onServerStopped);
        reg.registerOnServerTick(TickType.ON_120_TICKS, ChunkExplorerManager::onExploreTick);
        reg.registerOnServerTick(TickType.ON_1200_TICKS,  ChunkExplorerManager::on1200TicksSearchNewChunks);
    }

    //** GETTERS

    public int queueSize() {
        return chunkExploreQueue.size();
    }

    public int priorityQueueSize() {
        return chunkGenerationPriorityQueue.size();
    }

    /**
     * Submits a chunk to be generated during background gen. Priority chunks are
     * sprinkled into the explore queue on the next rebuild
     */
    public static void submitPriorityChunk(Level level, ChunkPos pos) {
        if (level == null || pos == null) return;
        if (GENERAL_CONFIG == null) return;

        ChunkExplorerManager manager = get(level);
        if (manager == null) return;

        synchronized (manager.chunkGenerationPriorityQueue) {
            if (manager.chunkGenerationPrioritySet.contains(pos)) return;
            manager.chunkGenerationPriorityQueue.addLast(pos);
            manager.chunkGenerationPrioritySet.add(pos);
        }
    }

    public static void submitPriorityChunks(Level level, Collection<ChunkPos> positions) {
        if (positions == null) return;
        for (ChunkPos pos : positions) submitPriorityChunk(level, pos);
    }

    //** PER-TICK LOGIC

    //Called each 120 ticks modulated against player set rate
    //Calls CHUNK_LOADER with force load implementation and checks held chunk for completion
    //tickChunk, forceTickChunk, forceload, forcechunk
    private void onTickForceLoadChunk(ServerTickEvent event)
    {
        MinecraftServer server = GENERAL_CONFIG.getServer();
        long now = GENERAL_CONFIG.getTotalTickCount();

        if(CHUNK_LOADER.isRunning((ServerLevel) level) ) {
            String msg = String.format("Skipping chunk explore, exploration in progress at chunk: %s, queue size: %d", heldChunk, chunkExploreQueue.size());
            LoggerBase.logInfo(null, "0370013", msg);
            return;
        }


        if (CHUNK_LOADER.unforceChunkLoad((ServerLevel) level, heldChunk) ) {
                heldChunk = null;
        }

        //aaa
        if (heldChunk == null) {
            ChunkPos next = pollNextUninitialized();
            if (next != null) {
                heldChunk = next;
                heldSince = now;
                requestedChunks.add(next);
                CHUNK_LOADER.forceChunkLoad((ServerLevel) level, next);
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
        final int priorityStride = Math.max(1, players.size());

        for (int i=0; i < CHUNK_EXPLORER_MAX; i++)
        {
            Player player = players.get(i % players.size());
            ChunkGenerationOrderHandler handler = chunkGenerators.get(player);
            ChunkPos next = handler.getNextUnInitSpiralChunk(util, SKIP_CHUNKS);
            if(next == null) break;
            if(handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS)) continue;
            chunkExploreQueue.addLast(next);

            //Sprinkle one submitted chunk in for every player's worth of spiral entries
            if((i + 1) % priorityStride != 0) continue;
            ChunkPos priority = pollNextPriorityChunk(players);
            if(priority != null) chunkExploreQueue.addLast(priority);
        }

    }


    /**
     * Takes the next submitted chunk still worth generating. Chunks already initialized or loaded are
     * dropped; chunks outside the scan radius of every player go to the back of the queue so they
     * become eligible again once a player moves closer.
     */
    private ChunkPos pollNextPriorityChunk(List<Player> players)
    {
        synchronized (chunkGenerationPriorityQueue)
        {
            int attempts = chunkGenerationPriorityQueue.size();
            for (int i = 0; i < attempts; i++)
            {
                ChunkPos candidate = chunkGenerationPriorityQueue.pollFirst();
                if (candidate == null) return null;

                if (util.isChunkInitialized(candidate)) continue;
                if (util.isLoaded(candidate)) continue;

                if (!withinExploreRange(candidate, players)) {
                    chunkGenerationPriorityQueue.addLast(candidate);
                    continue;
                }

                return candidate;
            }
        }
        return null;
    }

    private boolean withinExploreRange(ChunkPos pos, List<Player> players)
    {
        for (Player player : players) {
            ChunkPos playerChunk = new ChunkPos(player.blockPosition());
            if (Math.abs(pos.x - playerChunk.x) > SCAN_RADIUS_CHUNKS) continue;
            if (Math.abs(pos.z - playerChunk.z) > SCAN_RADIUS_CHUNKS) continue;
            return true;
        }
        return false;
    }


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
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        ChunkExplorerManager.initLevel((Level) event.getLevel());
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ChunkExplorerManager manager = managers.remove(event.getLevel());
        if (manager != null && manager.heldChunk != null) {
            CHUNK_LOADER.unforceChunkLoad((ServerLevel) manager.level, manager.heldChunk);
            manager.heldChunk = null;
        }
        if (CHUNK_LOADER != null) CHUNK_LOADER.restoreListeners();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        if (CHUNK_LOADER != null) CHUNK_LOADER.restoreListeners();
        managers.clear();
    }

    private static final int RATE_MIN = 1;
    private static final int RATE_MAX = 1000;
    private static int exploreTickCounter = 0;

    /** checks explore interval but does not tick it **/
    public static boolean checkExploreInterval() {
        return exploreTickCounter >= getExploreInterval();
    }

    /** ticks the explore interval and returns true if we have exceeded the interval time **/
    private static boolean tickExploreInterval() {
        if( ++exploreTickCounter > getExploreInterval() )
            exploreTickCounter = 1;
        return checkExploreInterval();
    }

    private static int getExploreInterval() {
        int rate = GENERAL_CONFIG.getPerformanceImpactConfig().getChunkExploreRate();
        rate = Math.max(RATE_MIN, Math.min(RATE_MAX, rate));
        return rate;
    }


    private static boolean exploreChunksEnabled() {
        return GENERAL_CONFIG.getPerformanceImpactConfig().isEnableChunkExplorer();
    }


    //Every initialized chunk is counted, used as estimate for disk data size
    private static long totalInitializedChunks() {
        long total = 0;
        for (ChunkExplorerManager manager : managers.values()) {
            total += manager.util.getInitializedChunkCount();
        }
        return total;
    }

    public static final int ALLOCATION_WINDOW_SECONDS = 60;

    private static boolean allocationRateExceeded() {
        PerformanceImpactConfig config = GENERAL_CONFIG.getPerformanceImpactConfig();
        int maxMb = config.getChunkExploreMaxAllocationRate();
        double averageMb = PerformanceImpactConfig.getAllocationRate(ALLOCATION_WINDOW_SECONDS);
        if( averageMb <= maxMb ) return false;

        LoggerBase.logWarning(null, CLASS_ID, "Chunk Explorer skipped load, heap allocation averaged "
            + String.format("%.0f", averageMb) + "MB/s over the last " + ALLOCATION_WINDOW_SECONDS
            + " seconds, above the " + maxMb + "MB/s budget. Raise chunkExploreMaxAllocationRate to continue exploring.");
        return true;
    }

    private static boolean averageTickTickTimeExceeded() {
        int millis =  GENERAL_CONFIG.getPerformanceImpactConfig().getChunkExploreMaxAllowedTickSize();
        if( GENERAL_CONFIG.getServer().getAverageTickTime() > millis ) {
            LoggerBase.logWarning(null, CLASS_ID, "Chunk Explorer skipped load, server is experiencing long ticks: "
                + "ms exceeds the " + millis + "ms budget. Reduce chunkExploreRate or increase chunkExploreMaxAllowedTickSize to continue exploring.");
            return true;
        }
        return false;
    }

    private static boolean diskLimitExceeded() {
        int maxGigabytes = GENERAL_CONFIG.getPerformanceImpactConfig().getChunkExploreMaximumDiskSize();
        long maxChunks = HBUtil.ChunkUtil.gigabytesToChunkCount(maxGigabytes);
        long total = totalInitializedChunks();
        if (total < maxChunks) return false;

        LoggerBase.logWarning(null, CLASS_ID, "Chunk Explorer halted: " + total
            + " initialized chunks exceeds the " + maxGigabytes + "GB budget of " + maxChunks
            + " chunks. Raise chunkExploreMaximumDiskSize to continue exploring.");
        return true;
    }

    //Calls managers to explore chunks by calling chunk handler (pregenerator mod or native)
    private static void onExploreTick(ServerTickEvent event)
    {
        if(!exploreChunksEnabled()) return;
        if (!tickExploreInterval()) return;

        if (diskLimitExceeded()) return;
        if (averageTickTickTimeExceeded()) return;
        if (allocationRateExceeded()) return;

        for (ChunkExplorerManager manager : managers.values()) {
            if(!CHUNK_LOADER.isRunningOnLevel((ServerLevel) manager.level)) continue;
            manager.onTickForceLoadChunk(event);
        }
    }

    private void releaseHolds() {
        if (heldChunk == null) return;
        long now = GENERAL_CONFIG.getTotalTickCount();
        long heldDuration = now - heldSince;
        if (heldDuration > 20) {
            LoggerBase.logInfo(null, CLASS_ID, "Releasing held chunk " + heldChunk + " after " + heldDuration + " ticks.");
            CHUNK_LOADER.unforceChunkLoad((ServerLevel) level, heldChunk);
            heldChunk = null;
        }
    }


    private static void on1200TicksSearchNewChunks(ServerTickEvent event)
    {
        if (GENERAL_CONFIG.getServer() == null) return;

        Map<Level, List<Player>> playersByLevel = new HashMap<>();
        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            playersByLevel
                .computeIfAbsent(player.serverLevel(), k -> new ArrayList<>())
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
                ChunkPos p = new ChunkPos(player.blockPosition());
                chunkGenerators.put(player, new ChunkGenerationOrderHandler(p, 0, SCAN_RADIUS_CHUNKS) );
            }
            ChunkGenerationOrderHandler handler = chunkGenerators.get(player);
            if(handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS)) {
                ChunkPos p = new ChunkPos(player.blockPosition());
                int skipped = Math.max(1, handler.getSkippedChunks());
                double skipRatio = (double) PLAYER_RENDER_DIST_SQ / skipped * SKIP_CHUNKS;
                int radialOffset = Math.max(0, Math.min(SCAN_RADIUS_CHUNKS, (int) skipRatio));
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

        /**
         * Advances the spiral until it lands on a chunk that has not been initialized yet.
         * Returns null once the walk leaves the scan radius.
         */
        public ChunkPos getNextUnInitSpiralChunk(ManagedChunkUtility util, int skipChunks)
        {
            if (total == 0) {
                total++;
                return currentPos;
            }

            int step = Math.max(1, skipChunks);
            int maxProbes = 4 * (radius / step + 1) * (radius / step + 1) + 8;

            for (int i = 0; i < maxProbes; i++)
            {
                stepSpiral(step);
                if (outsideScanRadius(currentPos)) return null;
                if (!util.isChunkInitialized(currentPos.x, currentPos.z)) return currentPos;
                skippedChunks++;
            }

            return null;
        }

        private void stepSpiral(int step)
        {
            if (dirCount == count) {
                dir = getNextDirection();
                dirCount = 0;
                // Leg length grows every half turn, so increment on the vertical legs only
                if (dir == UP || dir == DOWN) count++;
            }

            currentPos = new ChunkPos(currentPos.x + dir[0]*step, currentPos.z + dir[1]*step);
            total++;
            dirCount++;
        }

        private boolean outsideScanRadius(ChunkPos pos) {
            return Math.abs(pos.x - startPos.x) > radius || Math.abs(pos.z - startPos.z) > radius;
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
