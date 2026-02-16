package com.holybuckets.foundation.biome;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

public class BiomeManager {

    private Level level;
    private Registry<Biome> biomeRegistry; // Nullable - only available on server side
    private Map<BlockPos, BiomeInfo> biomes;
    private Map<ResourceLocation, Set<BlockPos>> biomesByType;

    private static Map<Level, BiomeManager> managers = new HashMap<>();

    private BiomeManager(Level level) {
        this.level = level;
        this.biomes = new HashMap<>();
        this.biomesByType = new HashMap<>();
        if (!level.isClientSide()) {
            this.biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        }
    }

    //** GETTERS

    public static ResourceLocation toLocation(String stringBiome) {
        return new ResourceLocation(stringBiome);
    }

    public Set<ResourceLocation> getAllBiomes() {
        if (biomeRegistry == null) return Set.of();
        return biomeRegistry.keySet();
    }

    public Map<BlockPos, BiomeInfo> getBiomes() {
        return biomes;
    }

    public List<BlockPos> getBiomePosByType(ResourceLocation location) {
        if (!biomesByType.containsKey(location)) return List.of();
        return List.copyOf(biomesByType.get(location));
    }

    public List<BiomeInfo> getBiomesByType(ResourceLocation location) {
        if (!biomesByType.containsKey(location)) return List.of();
        return biomesByType.get(location).stream()
                .map(pos -> biomes.get(pos))
                .filter(Objects::nonNull)
                .toList();
    }

    public BiomeInfo getNearestBiomeOfType(ResourceLocation location, BlockPos center) {
        if (!biomesByType.containsKey(location)) return null;
        return biomesByType.get(location).stream()
            .map(pos -> biomes.get(pos))
            .filter(Objects::nonNull)
            .min(Comparator.comparingDouble(info -> info.samplePos.distSqr(center)))
            .orElse(null);
    }

    public List<BiomeInfo> getNearestBiomes(BlockPos center, double maxDistance) {
        double maxDistSq = maxDistance * maxDistance;
        return biomes.values().stream()
            .filter(info -> info.samplePos.distSqr(center) <= maxDistSq)
            .sorted(Comparator.comparingDouble(a -> a.samplePos.distSqr(center)))
            .toList();
    }

    public List<BiomeInfo> getNearestBiomes(BlockPos center, int limit) {
        if (limit < 1) limit = biomes.size();
        return biomes.values().stream()
            .sorted(Comparator.comparingDouble(a -> a.samplePos.distSqr(center)))
            .limit(limit)
            .toList();
    }

    public List<BiomeInfo> getNearestWhitelistedBiomes(Set<ResourceLocation> whiteList, BlockPos center, int limit) {
        if (limit < 1) limit = biomes.size();
        List<BiomeInfo> allBiomes = new LinkedList<>();
        for (ResourceLocation location : whiteList) {
            var bs = getBiomesByType(location);
            if (bs == null) continue;
            allBiomes.addAll(bs.stream().map(pos -> biomes.get(pos)).filter(Objects::nonNull).toList());
        }
        return allBiomes.stream()
            .sorted(Comparator.comparingDouble(a -> a.samplePos.distSqr(center)))
            .limit(limit)
            .toList();
    }

    public List<BiomeInfo> getNearestBlacklistedBiomes(Set<ResourceLocation> blackList, BlockPos center, int limit) {
        if (limit < 1) limit = biomes.size();
        List<BiomeInfo> allBiomes = new LinkedList<>();
        for (BiomeInfo info : biomes.values()) {
            if (info.getId() != null && !blackList.contains(info.getId())) {
                allBiomes.add(info);
            }
        }
        return allBiomes.stream()
            .sorted(Comparator.comparingDouble(a -> a.samplePos.distSqr(center)))
            .limit(limit)
            .toList();
    }

    //** CHUNK HANDLING

    private void onChunkLoad(ChunkAccess chunk)
    {
        if (biomeRegistry == null) return;
        if(ManagedChunkUtility.isChunkLoaded(level, chunk.getPos())) return; // already loaded this chunk

        int chunkX = chunk.getPos().getMinBlockX() + 8;
        int chunkZ = chunk.getPos().getMinBlockZ() + 8;

        LevelChunkSection[] sections = chunk.getSections();
        Set<ResourceLocation> biomesInSection = new HashSet<>();
        for (int i = 0; i < sections.length; i++)
        {
            LevelChunkSection section = sections[i];
            if (section == null || section.getBiomes() == null) continue;
            if(section.hasOnlyAir()) continue;

            int sectionY = chunk.getSectionYFromSectionIndex(i);
            int worldY = level.getSectionYFromSectionIndex(i) * 16 + 8;

            // Sample the center of this section (biome palette is 4-block resolution)
            Holder<Biome> holder = HBUtil.LevelUtil.getBiomeFromSection(section, 8, 0, 8);
            if (holder == null) continue;

            ResourceLocation biomeId = holder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
            if (biomeId == null) continue;
            if(biomesInSection.contains(biomeId)) continue; // already sampled this biome in another section of the same chunk
            biomesInSection.add(biomeId);

            // Check if any adjacent chunk has the same biome already recorded
            List<ChunkPos> nearbyChunks = HBUtil.ChunkUtil.getLocalChunkPos(chunk.getPos(), 2);
            boolean skipBiome = false;
            for (ChunkPos nearbyPos : nearbyChunks) {
                BlockPos nearbyWorldPos = toWorldPos(nearbyPos, i);
                BiomeInfo nearbyBiome = biomes.get(nearbyWorldPos);
                if (nearbyBiome != null && biomeId.equals(nearbyBiome.getId())) {
                    skipBiome = true;
                    break;
                }
            }
            if (skipBiome) continue;

            BlockPos samplePos = new BlockPos(chunkX, worldY, chunkZ);
            if (biomes.containsKey(samplePos)) continue;

            BiomeInfo info = BiomeInfo.of(holder, samplePos, biomeRegistry);
            biomes.put(samplePos, info);
            biomesByType.computeIfAbsent(biomeId, k -> new HashSet<>()).add(samplePos);
        }
    }

    private void onChunkUnload(ChunkAccess chunk) {

    }

    //** PERSISTENCE

    private void load(DataStore ds) {
        if (!GeneralConfig.getInstance().isServerSide()) return;
        JsonElement root = ds.getOrCreateLevelSaveData(Constants.MOD_ID, this.level).get("biomes");
        if (root == null || root.isJsonNull()) return;

        JsonObject rootObj = root.getAsJsonObject();
        for (String key : rootObj.keySet()) {
            JsonArray arr = rootObj.getAsJsonArray(key);
            int registryId = Integer.parseInt(key);
            for (JsonElement elem : arr) {
                Biome biome = biomeRegistry.byId(registryId);
                if (biome == null) continue;
                ResourceLocation biomeId = biomeRegistry.getKey(biome);
                if (biomeId == null) continue;

                BiomeInfo info = BiomeInfo.of(registryId, elem.getAsString(), biomeRegistry);
                if (biomes.containsKey(info.samplePos)) continue;
                biomes.put(info.samplePos, info);
                biomesByType.computeIfAbsent(biomeId, k -> new HashSet<>()).add(info.samplePos);
            }
        }
    }

    private void save(DataStore ds) {
        JsonObject root = new JsonObject();

        for (BiomeInfo info : biomes.values())
        {
            CompoundTag tag = info.serialize();
            String key = tag.getInt("registryId") + "";
            if (!root.has(key)) root.add(key, new JsonArray());
            root.getAsJsonArray(key).add(tag.getString("samplePos"));
        }

        ds.getOrCreateLevelSaveData(Constants.MOD_ID, this.level).addProperty("biomes", root);
    }

    //** STATICS

    public static BlockPos toWorldPos(ChunkPos pos, int sectionIndex) {
        return pos.getBlockAt(0, sectionIndex*16, 0);
    }

    private static BiomeManager init(Level level) {
        if (!managers.containsKey(level)) {
            managers.put(level, new BiomeManager(level));
        }
        return managers.get(level);
    }

    public static BiomeManager get(Level level) {
        if (GeneralConfig.getInstance().isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!managers.containsKey(level))
            init(level);
        return managers.get(level);
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(BiomeManager::onServerStart);
        reg.registerOnLevelLoad(BiomeManager::onLevelLoad, EventPriority.High);
        reg.registerOnChunkLoad(BiomeManager::onChunkLoad);
        reg.registerOnChunkUnload(BiomeManager::onChunkUnload);
        reg.registerOnDataSave(BiomeManager::onDataSave);
    }

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        BiomeManager manager = BiomeManager.init((Level) event.getLevel());
        if (manager != null && !event.getLevel().isClientSide())
            manager.load(GeneralConfig.getInstance().getDataStore());
    }

    private static void onChunkLoad(ChunkLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        BiomeManager.get((Level) event.getLevel()).onChunkLoad(event.getChunk());
    }

    private static void onChunkUnload(ChunkLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        BiomeManager.get((Level) event.getLevel()).onChunkUnload(event.getChunk());
    }

    private static void onDataSave(DatastoreSaveEvent event) {
        for (BiomeManager manager : managers.values()) {
            manager.save(event.getDataStore());
        }
    }
}
