package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import javax.annotation.Nullable;
import java.util.*;

public class ChunkPriorityExplorer {

    public static final String CLASS_ID = "044";

    public static final int SEARCH_RADIUS = 12800;
    public static final int HORIZONTAL_STEP = 64;
    public static final int VERTICAL_STEP = 64;
    public static final int STRUCTURE_SEARCH_RINGS = 32;
    public static final float REFRESH_THRESHOLD = 0.5f;

    private static final Map<Level, ChunkPriorityExplorer> explorers = new HashMap<>();

    private final ServerLevel level;
    private final Set<Holder<Biome>> levelBiomes;
    private final Map<Holder<Biome>, ChunkPos> distantBiomes = new HashMap<>();
    private final int biomeRefreshSize;
    private final int structureRefreshSize;

    private final Set<Holder<Structure>> levelStructures = new HashSet<>();
    private final Map<Holder<Structure>, ChunkPos> distantStructures = new HashMap<>();
    private final Set<Holder<Structure>> missingStructures = new HashSet<>();
    private BlockPos cachedPos;
    private boolean biomeOrStructure = true;

    private ChunkPriorityExplorer(ServerLevel level)
    {
        this.level = level;
        this.levelBiomes = new HashSet<>(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
        this.biomeRefreshSize = Math.max(1, (int) (levelBiomes.size() * REFRESH_THRESHOLD));

        for (Holder<StructureSet> set : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                levelStructures.add(  entry.structure() );
            }
        }
        this.structureRefreshSize = Math.max(1, (int) (levelStructures.size() * REFRESH_THRESHOLD));
    }


    private void handleOnTick()
    {
        Player p = HBUtil.PlayerUtil.getRandomPlayer(level);
        biomeOrStructure = !biomeOrStructure;
        boolean idle = p == null;
        if (idle) {
            if (cachedPos == null) cachedPos = GeneralConfig.getInstance().getWorldSpawn(level);
            if(biomeOrStructure)
                exploreBiomes(cachedPos, SEARCH_RADIUS * 2);
            else
                exploreStructures(cachedPos, STRUCTURE_SEARCH_RINGS*2);
        } else {
            cachedPos = p.blockPosition();
            if(biomeOrStructure)
                exploreBiomes(cachedPos, SEARCH_RADIUS);
            else
                exploreStructures(cachedPos, STRUCTURE_SEARCH_RINGS);
        }

    }

    private void exploreBiomes(BlockPos origin, int searchRadius)
    {

        Set<Holder<Biome>> targets = new HashSet<>(levelBiomes);
        targets.removeAll(distantBiomes.keySet());
        if (targets.isEmpty()) {
            refreshBiomeList();
            return;
        }

        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(targets::contains, origin,
            searchRadius, HORIZONTAL_STEP, VERTICAL_STEP);
        if (found == null) {
            refreshBiomeList();
            return;
        }

        ChunkPos chunkPos = new ChunkPos(found.getFirst());
        ChunkExplorerManager.submitPriorityChunk(level, chunkPos);
        distantBiomes.put(found.getSecond(), chunkPos);
        LoggerBase.logDebug(null, "044000", "Discovered distant biome: " + HBUtil.LevelUtil.getBiomeSimpleName(found.getSecond()) + " at " + chunkPos);

        if (distantBiomes.size() >= biomeRefreshSize)
            refreshBiomeList();
    }

    private void exploreStructures(BlockPos origin, int searchRadius)
    {
        HolderSet<Structure> targets = nextUnexploredStructures();
        if (targets.size()==0) {
            refreshStructureList();
            return;
        }

        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, targets, origin, searchRadius, false);

        if (found == null) {
            refreshStructureList();
            return;
        }

        ChunkPos chunkPos = new ChunkPos(found.getFirst());
        ChunkExplorerManager.submitPriorityChunk(level, chunkPos);
        distantStructures.put(found.getSecond(), chunkPos);
        LoggerBase.logDebug(null, "044002", "Discovered distant structure: "
         + found.getSecond().unwrapKey() + " at " + chunkPos);


        if (distantBiomes.size() >= structureRefreshSize)
            refreshBiomeList();
    }

    private HolderSet.Direct<Structure> nextUnexploredStructures() {
        List<Holder<Structure>> unexplored = new ArrayList<>(levelStructures.size());
        for( Holder<Structure> s : levelStructures) {
            if(distantStructures.containsKey(s)) continue;
            if(missingStructures.contains(s)) continue;
            unexplored.add(s);
        }

        return HolderSet.direct(unexplored);
    }

    private void refreshBiomeList() {
        LoggerBase.logDebug(null, "044001", "Refreshing distant biome list for level: " + level.dimension().location());
        distantBiomes.clear();
    }

    private void refreshStructureList() {
        LoggerBase.logDebug(null, "044003", "Refreshing distant structure list for level: " + level.dimension().location());
        distantStructures.clear();
    }

    private void shutdown() {
        distantBiomes.clear();
        distantStructures.clear();
        missingStructures.clear();
    }

    public static ChunkPriorityExplorer get(ServerLevel serverLevel) {
        return explorers.computeIfAbsent(serverLevel, l -> new ChunkPriorityExplorer(serverLevel));
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(ChunkPriorityExplorer::onServerStart);
        reg.registerOnLevelLoad(ChunkPriorityExplorer::onLevelLoad);
        reg.registerOnLevelUnload(ChunkPriorityExplorer::onLevelUnload);
        reg.registerOnServerStopped(ChunkPriorityExplorer::onServerStopped);
        reg.registerOnServerTick(TickType.ON_120_TICKS, ChunkPriorityExplorer::on600Ticks);
    }

    private static final int INTERVAL = 5;
    private static int count = 0;

    private static void on600Ticks(ServerTickEvent event) {
        if(++count % INTERVAL != 0) return;
        explorers.get(GeneralConfig.OVERWORLD).handleOnTick();
    }

    private static void onServerStart(ServerStartingEvent event) {
        shutdownAll();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ResourceKey<Level> dimension = level.dimension();
        if (dimension.equals(Level.OVERWORLD) || dimension.equals(Level.NETHER) || dimension.equals(Level.END))
            get(level);
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChunkPriorityExplorer explorer = explorers.remove(level);
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
