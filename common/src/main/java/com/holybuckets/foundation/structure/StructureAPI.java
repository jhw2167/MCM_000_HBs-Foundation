package com.holybuckets.foundation.structure;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;

public class StructureAPI {

    StructureManager manager;

    public StructureAPI(Level level) throws NoSuchElementException {
        if(level == null || StructureManager.get(level) == null)
            throw new NoSuchElementException("No manager found for level");
        manager = StructureManager.get(level);
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


}
