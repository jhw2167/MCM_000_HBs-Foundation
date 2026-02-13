package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtility;
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

    // --- tunables ---
    public static final int SCAN_RADIUS_CHUNKS = 625;   // 10,000 blocks / 16
    public static final int HOLD_TICKS         = 2;     // ticks a probe stays loaded
    // ----------------

    private static final String TICKET_PREFIX = "chunk_explorer_";
    private static final Map<Level, ChunkExplorerManager> managers = new HashMap<>();

    private final Level level;

    // Ordered queue of positions still needing exploration this pass
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();

    // Single probe currently held open, and the tick it was loaded
    private ChunkPos heldChunk = null;
    private long     heldSince = Long.MIN_VALUE;

    private ChunkExplorerManager(Level level) {
        this.level = level;
    }

    //** GETTERS

    public int queueSize() {
        return queue.size();
    }

    //** PER-TICK LOGIC

    private void onTick(ServerTickEvent event) {
        long now = GeneralConfig.getInstance().getTotalTickCount();

        // 1. Release the held probe once listeners have had time to process it
        if (heldChunk != null && (now - heldSince) >= HOLD_TICKS) {
            HBUtil.ChunkUtil.unforceLoadChunk((ServerLevel) level, heldChunk, ticketId(heldChunk));
            heldChunk = null;
        }

        // 2. Advance to the next uninitialised position (1 per tick maximum)
        if (heldChunk == null) {
            ChunkPos next = pollNextUninitialized();
            if (next != null) {
                HBUtil.ChunkUtil.forceLoadChunk((ServerLevel) level, next, ticketId(next));
                heldChunk = next;
                heldSince = now;
            }
        }
    }

    /**
     * Drains the front of the queue, skipping any chunk that has since been
     * initialized organically (player walked in, loaded by another system, etc.)
     */
    private ChunkPos pollNextUninitialized() {
        ManagedChunkUtility util = ManagedChunkUtility.getInstance(level);
        Set<String> initialized = ManagedChunk.INITIALIZED_CHUNKS.get(level);

        while (!queue.isEmpty()) {
            ChunkPos candidate = queue.poll();
            String id = HBUtil.ChunkUtil.getId(candidate);

            boolean alreadyInitialized = (initialized != null && initialized.contains(id));
            if (!alreadyInitialized) {
                return candidate;
            }
        }
        return null;
    }

    //** QUEUE REBUILD (every 1200 ticks)

    /**
     * For each player position, generate an outward spiral of ChunkPos probes
     * up to SCAN_RADIUS_CHUNKS. Merge all spirals into one deduplicated,
     * distance-sorted queue, excluding already-initialized chunks.
     */
    private void rebuildQueue(List<BlockPos> playerPositions)
    {
        if (playerPositions.isEmpty()) return;

        Set<String> initialized = ManagedChunkUtility.
        Set<String>       seen       = new LinkedHashSet<>();
        List<ScoredChunk> candidates = new ArrayList<>();

        for (BlockPos origin : playerPositions) {
            ChunkGenerationOrderHandler handler =
                new ChunkGenerationOrderHandler(new ChunkPos(origin));

            // Walk the spiral until we exceed the scan radius
            while (!handler.testScanRadiusExceeded(SCAN_RADIUS_CHUNKS)) {
                ChunkPos probe = handler.getNextSpiralChunk();
                String id = HBUtil.ChunkUtil.getId(probe);

                if (seen.contains(id)) continue;
                seen.add(id);
                if (initialized != null && initialized.contains(id)) continue;

                // Score = min squared chunk distance to any player
                long minDistSq = Long.MAX_VALUE;
                for (BlockPos p : playerPositions) {
                    ChunkPos pc = new ChunkPos(p);
                    long dx = probe.x - pc.x;
                    long dz = probe.z - pc.z;
                    minDistSq = Math.min(minDistSq, dx * dx + dz * dz);
                }
                candidates.add(new ScoredChunk(probe, minDistSq));
            }
        }

        // Nearest chunks first across all player origins
        candidates.sort(Comparator.comparingLong(sc -> sc.distSq));

        // Release any in-flight probe, then swap the queue
        if (heldChunk != null) {
            HBUtil.ChunkUtil.unforceLoadChunk((ServerLevel) level, heldChunk, ticketId(heldChunk));
            heldChunk = null;
        }
        queue.clear();
        for (ScoredChunk sc : candidates) {
            queue.add(sc.pos);
        }
    }

    //** HELPERS

    private static String ticketId(ChunkPos pos) {
        return TICKET_PREFIX + pos.x + "_" + pos.z;
    }

    private record ScoredChunk(ChunkPos pos, long distSq) {}

    //** STATICS

    public static ChunkExplorerManager get(Level level) {
        if (GeneralConfig.getInstance().isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!managers.containsKey(level))
            init(level);
        return managers.get(level);
    }

    private static ChunkExplorerManager init(Level level) {
        if (!managers.containsKey(level)) {
            managers.put(level, new ChunkExplorerManager(level));
        }
        return managers.get(level);
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(ChunkExplorerManager::onServerStart);
        reg.registerOnLevelLoad(ChunkExplorerManager::onLevelLoad);
        reg.registerOnLevelUnload(ChunkExplorerManager::onLevelUnload);
        reg.registerOnServerTick(TickType.ON_SINGLE_TICK, ChunkExplorerManager::onSingleTick);
        reg.registerOnServerTick(TickType.ON_1200_TICKS,  ChunkExplorerManager::on1200Ticks);
    }

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        ChunkExplorerManager.init((Level) event.getLevel());
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
        if (GeneralConfig.getInstance().getServer() == null) return;

        Map<Level, List<BlockPos>> playersByLevel = new HashMap<>();
        for (ServerPlayer player : GeneralConfig.getInstance().getServer().getPlayerList().getPlayers()) {
            playersByLevel
                .computeIfAbsent(player.serverLevel(), k -> new ArrayList<>())
                .add(player.blockPosition());
        }

        for (Map.Entry<Level, ChunkExplorerManager> entry : managers.entrySet()) {
            List<BlockPos> positions = playersByLevel.getOrDefault(entry.getKey(), List.of());
            if (!positions.isEmpty()) {
                entry.getValue().rebuildQueue(positions);
            }
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