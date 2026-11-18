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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import io.netty.util.collection.IntObjectHashMap;
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
    public static final int CHUNK_EXPLORER_MAX = 128;  // safety cap to prevent OOM if something goes wrong with the queue rebuild
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

    private final ExploreStats stats = new ExploreStats();
    private ChunkPos lastLoadedChunk = null;

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
        reg.registerOnServerTick(TickType.ON_20_TICKS, ChunkExplorerManager::onExploreTick);
        reg.registerOnServerTick(TickType.ON_1200_TICKS,  ChunkExplorerManager::on1200TicksSearchNewChunks);
        reg.registerOnServerTick(TickType.ON_6000_TICKS, ChunkExplorerManager::on6000TicksHealthCheck);
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
            stats.record(EVENT_SKIP_LOADER_BUSY);
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
                boolean accepted = CHUNK_LOADER.forceChunkLoad((ServerLevel) level, next);
                stats.record(accepted ? EVENT_LOAD_STARTED : EVENT_LOAD_REJECTED);
                if (accepted) lastLoadedChunk = next;
            } else {
                stats.record(EVENT_QUEUE_EXHAUSTED);
            }
        }
    }

    private ChunkPos pollNextUninitialized()
    {
        while (!chunkExploreQueue.isEmpty())
        {
            ChunkPos candidate = chunkExploreQueue.poll();
            if (candidate == null) {
                stats.record(EVENT_POLLED_NULL_ENTRY);
                return  null;
            }
            releasePriorityChunk(candidate);
            if (util.isChunkInitialized(candidate)) {
                stats.record(EVENT_POLL_SKIPPED_INITIALIZED);
                continue;
            }
            if (util.isLoaded(candidate)) {
                stats.record(EVENT_POLL_SKIPPED_LOADED);
                continue;
            }

            return candidate;
        }

        return null;
    }

    private static int SKIP_CHUNKS = 8;
    //** QUEUE REBUILD (every 1200 ticks)
    private static int pcount=0;
    private void rebuildQueue()
    {
        List<Player> players = new ArrayList<>(chunkGenerators.keySet());
        Player player = players.isEmpty() ? null : players.get(pcount++ % players.size());
        ChunkGenerationOrderHandler handler = (player == null) ? null : chunkGenerators.get(player);
        ChunkPos playerPos = (player == null) ? null : player.chunkPosition();

        synchronized (chunkGenerationPriorityQueue)
        {
            requeueUnprocessedPriorityChunks();
            chunkExploreQueue.clear();

            LinkedList<ChunkPos> priority = getClosestPriorityChunks(player);
            Set<ChunkPos> taken = new HashSet<>();

            for (int i = 0; i < CHUNK_EXPLORER_MAX; i++)
            {
                ChunkPos current = (handler == null) ? null : handler.getCurrentPos();
                ChunkPos priorityChunk = priority.peekFirst();
                if (current == null && priorityChunk == null) break;

                //take the closest by dist, priority chunks win ties
                boolean takePriority = (priorityChunk==null);
                if(!takePriority)
                    takePriority = (current==null);
                if(!takePriority) {
                    long d1 = HBUtil.ChunkUtil.chunkDistSquared(playerPos, priorityChunk);
                    long d2 = HBUtil.ChunkUtil.chunkDistSquared(playerPos, current);
                    takePriority = d1<=d2;
                }

                if (takePriority) {
                    chunkExploreQueue.add(priority.pollFirst());
                    taken.add(priorityChunk);
                } else {
                    chunkExploreQueue.add(current);
                    handler.getNextUnInitSpiralChunk(util, SKIP_CHUNKS);
                }
            }

            taken.remove(null);
            if (!taken.isEmpty()) {
                chunkGenerationPriorityQueue.removeIf(taken::contains);
            }
            stats.record(EVENT_PRIORITY_QUEUED, taken.size());
        }
        stats.record(EVENT_QUEUE_REBUILT);
    }

    private void requeueUnprocessedPriorityChunks() {
        int discarded = 0;
        int requeued = 0;
        Iterator<ChunkPos> it = chunkExploreQueue.descendingIterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            if (pos == null) continue;
            if (chunkGenerationPrioritySet.contains(pos)) {
                chunkGenerationPriorityQueue.addFirst(pos);
                requeued++;
            } else {
                discarded++;
            }
        }
        stats.record(EVENT_DISCARDED_ON_REBUILD, discarded);
        stats.record(EVENT_PRIORITY_REQUEUED_ON_REBUILD, requeued);
    }

    private void releasePriorityChunk(ChunkPos pos) {
        synchronized (chunkGenerationPriorityQueue) {
            chunkGenerationPrioritySet.remove(pos);
        }
    }

        private LinkedList<ChunkPos> getClosestPriorityChunks(Player p) {
            LinkedList<ChunkPos> sorted = new LinkedList<>(chunkGenerationPriorityQueue);
            if (p == null) return sorted;
            ChunkPos playerPos = p.chunkPosition();
            sorted.sort(Comparator.comparingInt(c -> HBUtil.ChunkUtil.chunkDistSquared(playerPos, c)));
            return sorted;
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

    private static boolean isExplorableLevel(Level level) {
        return Level.OVERWORLD.equals(level.dimension()) || Level.NETHER.equals(level.dimension());
    }

    private static ChunkExplorerManager initLevel(Level level) {
        if (!isExplorableLevel(level)) return null;
        if (!managers.containsKey(level)) {
            managers.put(level, new ChunkExplorerManager(level));
        }
        return managers.get(level);
    }

    //** EVENTS

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
        GLOBAL_STATS.reset();
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
        return rate * (( rate/10)+1); //parabolic rate
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
        GLOBAL_STATS.record(EVENT_INTERVAL_REACHED);

        if (diskLimitExceeded()) {
            GLOBAL_STATS.record(EVENT_SKIP_DISK_LIMIT);
            return;
        }
        if (averageTickTickTimeExceeded()) {
            GLOBAL_STATS.record(EVENT_SKIP_TICK_TIME);
            return;
        }
        if (allocationRateExceeded()) {
            GLOBAL_STATS.record(EVENT_SKIP_ALLOCATION);
            return;
        }

        for (ChunkExplorerManager manager : managers.values()) {
            if(!CHUNK_LOADER.isRunningOnLevel((ServerLevel) manager.level)) {
                manager.stats.record(EVENT_SKIP_LOADER_UNAVAILABLE);
                continue;
            }
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

    public static final long HEALTH_WINDOW_TICKS = 6000;
    private static final Gson HEALTH_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExploreStats GLOBAL_STATS = new ExploreStats();

    private static final String EVENT_INTERVAL_REACHED = "intervalReached";
    private static final String EVENT_SKIP_DISK_LIMIT = "skippedDiskLimit";
    private static final String EVENT_SKIP_TICK_TIME = "skippedTickTime";
    private static final String EVENT_SKIP_ALLOCATION = "skippedAllocationRate";
    private static final String EVENT_SKIP_LOADER_UNAVAILABLE = "skippedLoaderUnavailable";
    private static final String EVENT_SKIP_LOADER_BUSY = "skippedLoaderBusy";
    private static final String EVENT_LOAD_STARTED = "loadStarted";
    private static final String EVENT_LOAD_REJECTED = "loadRejectedByLoader";
    private static final String EVENT_QUEUE_EXHAUSTED = "pollReturnedNothing";
    private static final String EVENT_POLLED_NULL_ENTRY = "polledNullEntry";
    private static final String EVENT_POLL_SKIPPED_INITIALIZED = "pollSkippedInitialized";
    private static final String EVENT_POLL_SKIPPED_LOADED = "pollSkippedLoaded";
    private static final String EVENT_QUEUE_REBUILT = "queueRebuilt";
    private static final String EVENT_PRIORITY_QUEUED = "priorityMovedToExploreQueue";
    private static final String EVENT_DISCARDED_ON_REBUILD = "discardedOnRebuild";
    private static final String EVENT_PRIORITY_REQUEUED_ON_REBUILD = "priorityRequeuedOnRebuild";

    public static JsonObject healthCheckAll() {
        JsonObject all = new JsonObject();
        for (ChunkExplorerManager manager : new ArrayList<>(managers.values())) {
            all.add(HBUtil.LevelUtil.toLevelId(manager.level), manager.healthCheck());
        }
        return all;
    }

    public static JsonObject healthCheck(Level level) {
        if (level == null) return null;
        ChunkExplorerManager manager = managers.get(level);
        return (manager == null) ? null : manager.healthCheck();
    }

    private static void on6000TicksHealthCheck(ServerTickEvent event) {
        if (!exploreChunksEnabled()) return;
        JsonObject all = healthCheckAll();
        for (String levelId : all.keySet()) {
            logHealthCheck(levelId, all.getAsJsonObject(levelId));
        }
    }

    public static void logHealthCheck(String levelId, JsonObject health) {
        LoggerBase.logInfo(null, "037020", "Chunk Explorer Health Check for level: " + levelId
            + "\n\n" + HEALTH_GSON.toJson(health));
    }

    public JsonObject healthCheck() {
        JsonObject health = new JsonObject();
        health.addProperty("level", HBUtil.LevelUtil.toLevelId(level));
        health.addProperty("exploreRate", GENERAL_CONFIG.getPerformanceImpactConfig().getChunkExploreRate());
        health.addProperty("exploreQueue", chunkExploreQueue.size());
        synchronized (chunkGenerationPriorityQueue) {
            health.addProperty("priorityQueue", chunkGenerationPriorityQueue.size());
        }
        health.addProperty("initializedChunks", util.getInitializedChunkCount());
        return health;
    }

    private static class ExploreStats {
        private final Map<String, Deque<long[]>> recent = new HashMap<>();
        private final Map<String, Long> totals = new HashMap<>();
        private long startTick = Long.MIN_VALUE;

        void record(String event) {
            record(event, 1);
        }

        void record(String event, long amount) {
            if (GENERAL_CONFIG == null) return;
            long now = GENERAL_CONFIG.getTotalTickCount();
            if (startTick == Long.MIN_VALUE) startTick = now;
            recent.computeIfAbsent(event, k -> new ArrayDeque<>()).addLast(new long[]{now, amount});
            totals.merge(event, amount, Long::sum);
            prune(now);
        }

        void reset() {
            recent.clear();
            totals.clear();
            startTick = Long.MIN_VALUE;
        }

        long windowTicks(long now) {
            if (startTick == Long.MIN_VALUE) return 0;
            return Math.min(HEALTH_WINDOW_TICKS, now - startTick);
        }

        long recentCount(String event, long now) {
            prune(now);
            Deque<long[]> entries = recent.get(event);
            if (entries == null) return 0;
            long sum = 0;
            for (long[] entry : entries) sum += entry[1];
            return sum;
        }

        private void prune(long now) {
            for (Deque<long[]> entries : recent.values()) {
                while (!entries.isEmpty() && now - entries.peekFirst()[0] > HEALTH_WINDOW_TICKS) entries.pollFirst();
            }
        }

        JsonObject toJson(long now) {
            prune(now);
            JsonObject window = new JsonObject();
            for (String event : recent.keySet()) window.addProperty(event, recentCount(event, now));
            JsonObject lifetime = new JsonObject();
            totals.forEach(lifetime::addProperty);
            JsonObject json = new JsonObject();
            json.addProperty("windowTicks", windowTicks(now));
            json.add("window", window);
            json.add("lifetime", lifetime);
            return json;
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

        public ChunkPos getCurrentPos() {
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
