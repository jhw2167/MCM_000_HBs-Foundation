package com.holybuckets.foundation.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class StructureAPI {

    StructureManager manager;

    public StructureAPI(Level level) throws NoSuchElementException {
        if(level == null || StructureManager.get(level) == null)
            throw new NoSuchElementException("No manager found for level");
        manager = StructureManager.get(level);
    }

    public static @Nullable StructureAPI get(Level level) {
        try {
            return new StructureAPI(level);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public List<StructureInfo> nearestStructures(BlockPos pos, int limit) {
        return manager.getNearestStructures(pos, limit);
    }

    public List<StructureInfo> nearestStructuresOfType(BlockPos pos, ResourceLocation sType,  int limit) {
        Set<ResourceLocation> set = Set.of(sType);
        return manager.getNearestWhitelistedStructures(set, pos, limit);
    }

    public List<StructureInfo> nearestStructuresDistinct(BlockPos pos, int limit) {
        List<StructureInfo> nearest = manager.getNearestStructures(pos, limit);
        Set<ResourceLocation> set = new HashSet<>();
        List<StructureInfo> distinct = new ArrayList<>(nearest.size());
        for(StructureInfo info : nearest) {
            if(!set.contains(info.getId())) {
                distinct.add(info);
                set.add(info.getId());
            }
        }
        return distinct;
    }


    public Set<ResourceLocation> getAllStructures() {
        return manager.getAllStructures();
    }

    public List<StructureInfo> nearestStructuresOfTypes(BlockPos pos, Set<ResourceLocation> whiteList, int limit) {
        if(whiteList == null || whiteList.isEmpty()) return List.of();
        return manager.getNearestWhitelistedStructures(whiteList, pos, limit);
    }

    public List<StructureInfo> nearestStructuresExcluding(BlockPos pos, Set<ResourceLocation> blackList, int limit) {
        if(blackList == null) blackList = Set.of();
        return manager.getNearestBlackListedStructures(blackList, pos, limit);
    }

    public List<StructureInfo> structuresWithin(BlockPos pos, double maxDistance) {
        return manager.getNearestStructures(pos, maxDistance);
    }

    public List<StructureInfo> structuresInBand(BlockPos pos, double minDistance, double maxDistance, int limit) {
        return manager.getStructuresInBand(pos, minDistance, maxDistance, limit);
    }

    public List<StructureInfo> structuresInBandOfTypes(BlockPos pos, Set<ResourceLocation> whiteList,
                                                       double minDistance, double maxDistance, int limit) {
        if(whiteList == null || whiteList.isEmpty()) return List.of();
        return manager.getWhitelistedStructuresInBand(whiteList, pos, minDistance, maxDistance, limit);
    }

    public List<StructureInfo> structuresInBandExcluding(BlockPos pos, Set<ResourceLocation> blackList,
                                                         double minDistance, double maxDistance, int limit) {
        if(blackList == null) blackList = Set.of();
        return manager.getBlacklistedStructuresInBand(blackList, pos, minDistance, maxDistance, limit);
    }

    public List<StructureInfo> structuresOfType(ResourceLocation sType) {
        if(sType == null) return List.of();
        return manager.getStructuresByType(sType);
    }

    public @Nullable StructureInfo nearestStructure(BlockPos pos) {
        List<StructureInfo> nearest = manager.getNearestStructures(pos, 1);
        return nearest.isEmpty() ? null : nearest.get(0);
    }

    public @Nullable StructureInfo structureAt(BlockPos pos) {
        return manager.getStructureAt(pos);
    }

    public boolean isStructureLoaded(BlockPos pos) {
        return manager.isStructureLoaded(pos);
    }

    public @Nullable StructureStart loadedStructureStart(BlockPos pos) {
        return manager.getLoadedStructureStart(pos);
    }

    public @Nullable StructureStart loadedStructureStart(StructureInfo info) {
        return info == null ? null : manager.getLoadedStructureStart(info.getOrigin());
    }

    public boolean isDiscovered(ResourceLocation sType) {
        return manager.isDiscovered(sType);
    }

    public Set<ResourceLocation> getDiscoveredTypes() {
        return manager.getDiscoveredTypes();
    }

    public int getDiscoveredCount() {
        return manager.getDiscoveredCount();
    }

    public int getDiscoveredCount(ResourceLocation sType) {
        return manager.getDiscoveredCount(sType);
    }

    public void addPseudoStructure(ResourceLocation sType, BlockPos pos) {
        manager.addPseudoStructure(sType, pos);
    }

    public void removePseudoStructure(BlockPos pos) {
        manager.removePseudoStructure(pos);
    }

    public void syncPseudoStructures(Predicate<ResourceLocation> ownsType, Map<BlockPos, ResourceLocation> entries) {
        manager.syncPseudoStructures(ownsType, entries);
    }

    public static boolean addPseudoStructure(Level level, ResourceLocation sType, BlockPos pos) {
        StructureAPI api = get(level);
        if(api == null || sType == null || pos == null) return false;
        api.addPseudoStructure(sType, pos);
        StructureInfo info = api.structureAt(pos);
        return info != null && info.isPseudo() && sType.equals(info.getStructureLocation());
    }

    public static boolean removePseudoStructure(Level level, BlockPos pos) {
        StructureAPI api = get(level);
        if(api == null || pos == null) return false;
        StructureInfo info = api.structureAt(pos);
        if(info == null || !info.isPseudo()) return false;
        api.removePseudoStructure(pos);
        return true;
    }

    public static boolean syncPseudoStructures(Level level, Predicate<ResourceLocation> ownsType, Map<BlockPos, ResourceLocation> entries) {
        StructureAPI api = get(level);
        if(api == null) return false;
        api.syncPseudoStructures(ownsType, entries);
        return true;
    }

}
