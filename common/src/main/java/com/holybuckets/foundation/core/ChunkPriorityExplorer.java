package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChunkPriorityExplorer {

    public static final String CLASS_ID = "044";

    public static final int SEARCH_RADIUS = 6400;
    public static final int HORIZONTAL_STEP = 32;
    public static final int VERTICAL_STEP = 64;
    public static final long SEARCH_TIMEOUT_MS = 1000L;

    private static final Map<Level, ChunkPriorityExplorer> explorers = new ConcurrentHashMap<>();
    private static GeneralConfig GENERAL_CONFIG;

    private final ServerLevel level;
    private final Map<Holder<Biome>, ChunkPos> biomeChunks = new ConcurrentHashMap<>();
    private final Set<Holder<Biome>> discoveredBiomes = ConcurrentHashMap.newKeySet();
    private final ExecutorService searchExecutor;
    private CompletableFuture<Pair<BlockPos, Holder<Biome>>> runningSearch;
    private int playerIndex;

    private ChunkPriorityExplorer(ServerLevel level) {
        this.level = level;
        String threadName = "HBs-ChunkPriorityExplorer-" + level.dimension().location();
        this.searchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    public Map<Holder<Biome>, ChunkPos> getBiomeChunks() {
        return Collections.unmodifiableMap(biomeChunks);
    }

    @Nullable
    public ChunkPos getChunkPos(Holder<Biome> biome) {
        return biomeChunks.get(biome);
    }

    public boolean isDiscovered(Holder<Biome> biome) {
        return discoveredBiomes.contains(biome);
    }

    public boolean isSearching() {
        return runningSearch != null && !runningSearch.isDone();
    }

    private Set<Holder<Biome>> getUnexploredBiomes() {
        Set<Holder<Biome>> biomes = new HashSet<>(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
        biomes.removeAll(biomeChunks.keySet());
        biomes.removeAll(discoveredBiomes);
        return biomes;
    }

    @Nullable
    private ServerPlayer nextPlayer() {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return null;
        playerIndex = (playerIndex + 1) % players.size();
        return players.get(playerIndex);
    }

    private void explore() {
        if (searchExecutor.isShutdown() || isSearching()) return;
        Set<Holder<Biome>> targets = getUnexploredBiomes();
        if (targets.isEmpty()) return;
        ServerPlayer player = nextPlayer();
        if (player == null) return;

        BlockPos origin = player.blockPosition();
        runningSearch = CompletableFuture.supplyAsync(
            () -> level.findClosestBiome3d(targets::contains, origin, SEARCH_RADIUS, HORIZONTAL_STEP, VERTICAL_STEP),
            searchExecutor
        );
        runningSearch.copy()
            .orTimeout(SEARCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .whenComplete((result, error) -> {
                MinecraftServer server = level.getServer();
                if (server.isRunning()) server.execute(() -> onSearchComplete(origin, result, error));
            });
    }

    private void onSearchComplete(BlockPos origin, @Nullable Pair<BlockPos, Holder<Biome>> result, @Nullable Throwable error) {
        if (explorers.get(level) != this) return;
        String dimension = level.dimension().location().toString();
        if (error != null) {
            Throwable cause = (error instanceof CompletionException && error.getCause() != null) ? error.getCause() : error;
            if (cause instanceof TimeoutException)
                LoggerBase.logDebug(null, CLASS_ID + "001", "Biome search from " + origin + " in " + dimension + " timed out after " + SEARCH_TIMEOUT_MS + "ms");
            else
                LoggerBase.logError(null, CLASS_ID + "002", "Biome search from " + origin + " in " + dimension + " failed: " + cause.getMessage());
            return;
        }
        if (result == null) return;

        Holder<Biome> biome = result.getSecond();
        ChunkPos chunkPos = new ChunkPos(result.getFirst());
        ManagedChunkUtility chunkUtility = ManagedChunkUtility.getInstance(level);
        if (chunkUtility != null && chunkUtility.isChunkInitialized(chunkPos)) {
            discoveredBiomes.add(biome);
            return;
        }

        biomeChunks.put(biome, chunkPos);
        LoggerBase.logDebug(null, CLASS_ID + "003", "Cached " + HBUtil.LevelUtil.getBiomeSimpleName(biome) + " at chunk " + chunkPos + " in " + dimension);
    }

    private void shutdown() {
        searchExecutor.shutdownNow();
        biomeChunks.clear();
        discoveredBiomes.clear();
    }

    @Nullable
    public static ChunkPriorityExplorer get(Level level) {
        if (level == null) return null;
        if (GENERAL_CONFIG.isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return explorers.computeIfAbsent(serverLevel, l -> new ChunkPriorityExplorer(serverLevel));
    }

    public static void init(EventRegistrar reg) {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        reg.registerOnBeforeServerStarted(ChunkPriorityExplorer::onServerStart);
        reg.registerOnLevelLoad(ChunkPriorityExplorer::onLevelLoad);
        reg.registerOnLevelUnload(ChunkPriorityExplorer::onLevelUnload);
        reg.registerOnServerStopped(ChunkPriorityExplorer::onServerStopped);
        reg.registerOnServerTick(TickType.ON_1200_TICKS, ChunkPriorityExplorer::on1200Ticks);
    }

    private static void on1200Ticks(ServerTickEvent event) {
        for (ChunkPriorityExplorer explorer : explorers.values()) {
            explorer.explore();
        }
    }

    private static void onServerStart(ServerStartingEvent event) {
        shutdownAll();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        get((Level) event.getLevel());
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ChunkPriorityExplorer explorer = explorers.remove(event.getLevel());
        if (explorer != null) explorer.shutdown();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        shutdownAll();
    }

    private static void shutdownAll() {
        explorers.values().forEach(ChunkPriorityExplorer::shutdown);
        explorers.clear();
    }

}
