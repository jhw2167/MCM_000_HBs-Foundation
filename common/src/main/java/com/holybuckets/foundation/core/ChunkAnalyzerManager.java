package com.holybuckets.foundation.core;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.config.ModConfig;
import com.holybuckets.foundation.core.ChunkAnalyzer.ChunkBlockDistro;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChunkAnalyzerManager
 *
 * Samples 1 in every ModConfig.analyzeRate loaded chunks, analyzes one queued chunk every 20 ticks
 * with ChunkAnalyzer and logs the result. Chunks that unloaded before their turn are force loaded,
 * analyzed on a later tick, then released. A rate of 0 disables sampling.
 */
public class ChunkAnalyzerManager {

    public static final String CLASS_ID = "043";

    public static final int MAX_QUEUE_SIZE = 256;
    public static final int MAX_LOAD_ATTEMPTS = 10;
    public static final int LOG_LIMIT = -1;

    private static final Map<Level, ChunkAnalyzerManager> managers = new ConcurrentHashMap<>();
    private static GeneralConfig GENERAL_CONFIG;

    private final ServerLevel level;
    private final Deque<ChunkPos> analyzeQueue = new ConcurrentLinkedDeque<>();


    private ChunkAnalyzerManager(ServerLevel level) {
        this.level = level;
    }

    public static void init(EventRegistrar reg) {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        reg.registerOnBeforeServerStarted(ChunkAnalyzerManager::onServerStart);
        reg.registerOnLevelLoad(ChunkAnalyzerManager::onLevelLoad);
        reg.registerOnLevelUnload(ChunkAnalyzerManager::onLevelUnload);
        reg.registerOnServerStopped(ChunkAnalyzerManager::onServerStopped);
        reg.registerOnChunkLoad(ChunkAnalyzerManager::onChunkLoad, EventPriority.Low);
        reg.registerOnServerTick(TickType.ON_20_TICKS, ChunkAnalyzerManager::on20Ticks);
    }

    //** GETTERS

    public static int getAnalyzeRate() {
        return ModConfig.getInstance().getAnalyzeRate();
    }

    public static boolean isEnabled() {
        return getAnalyzeRate() > 0;
    }

    public int queueSize() {
        return analyzeQueue.size();
    }

    public static ChunkAnalyzerManager get(Level level) {
        if (level == null) return null;
        if (GENERAL_CONFIG.isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return managers.computeIfAbsent(serverLevel, l -> new ChunkAnalyzerManager(serverLevel));
    }

    //** QUEUE

    private void offer(ChunkPos pos) {
        int rate = getAnalyzeRate();
        if (rate <= 0) return;
        analyzeQueue.addLast(pos);
    }

    private void tick()
    {
        ChunkPos pos = analyzeQueue.peekFirst();
        if (pos == null) return;

        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            HBUtil.ChunkUtil.forceLoadChunk(level, pos, Constants.MOD_ID);
            return;
        }

        analyzeQueue.pollFirst();
        try {
            ChunkBlockDistro distro = ChunkAnalyzer.blockDistribution(level, pos);
            ChunkAnalyzer.log(distro, 0, LOG_LIMIT);
            HBUtil.ChunkUtil.unforceLoadChunk(level, pos, Constants.MOD_ID);
        } catch (Exception e) {
            LoggerBase.logError(null, CLASS_ID + "003", "Failed to analyze chunk " + pos + ": " + e.getMessage());
        }
    }

    private void clear() {
        analyzeQueue.clear();
    }

    //** EVENTS

    private static void onChunkLoad(ChunkLoadingEvent.Load event) {
        if (!isEnabled()) return;
        if (event.getLevel() == null || event.getLevel().isClientSide()) return;
        ChunkAnalyzerManager manager = get((Level) event.getLevel());
        if (manager != null && event.getChunk() != null) manager.offer(event.getChunk().getPos());
    }

    private static void on20Ticks(ServerTickEvent event) {
        boolean enabled = isEnabled();
        for (ChunkAnalyzerManager manager : managers.values()) {
            if (enabled) manager.tick();
            else if (!manager.analyzeQueue.isEmpty()) manager.clear();
        }
    }

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        get((Level) event.getLevel());
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ChunkAnalyzerManager manager = managers.remove(event.getLevel());
        if (manager != null) manager.clear();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        managers.clear();
    }

}
//END CLASS CHUNKANALYZERMANAGER
