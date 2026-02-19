package com.holybuckets.foundation.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class BiomeAPI {

    BiomeManager manager;

    public BiomeAPI(Level level) throws NoSuchElementException {
        if (level == null || BiomeManager.get(level) == null)
            throw new NoSuchElementException("No BiomeManager found for level");
        manager = BiomeManager.get(level);
    }

    public static @Nullable BiomeAPI get(Level level) {
        try {
            return new BiomeAPI(level);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public List<BiomeInfo> nearestBiomes(BlockPos pos, int limit) {
        return manager.getNearestBiomes(pos, limit);
    }

    public List<BiomeInfo> nearestBiomesOfType(BlockPos pos, ResourceLocation biomeType, int limit) {
        Set<ResourceLocation> set = Set.of(biomeType);
        return manager.getNearestWhitelistedBiomes(set, pos, limit);
    }

    /** Returns the nearest biomes with no duplicate types (one entry per ResourceLocation). */
    public List<BiomeInfo> nearestBiomesDistinct(BlockPos pos, int limit) {
        List<BiomeInfo> nearest = manager.getNearestBiomes(pos, limit * 8); // over-fetch to find distinct
        Set<ResourceLocation> seen = new HashSet<>();
        List<BiomeInfo> distinct = new ArrayList<>(limit);
        for (BiomeInfo info : nearest) {
            if (!seen.contains(info.getId())) {
                distinct.add(info);
                seen.add(info.getId());
                if (distinct.size() >= limit) break;
            }
        }
        return distinct;
    }

    /** Returns the nearest biomes with no duplicate types (one entry per ResourceLocation). */
    public List<BiomeInfo> getBiomesInChunkRange(BlockPos pos, int chunkRange) {
        return manager.getBiomesInChunkRange(pos, chunkRange);
    }

    public Set<ResourceLocation> getAllBiomes() {
        return manager.getAllBiomes();
    }
}
