package com.holybuckets.foundation.structure;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

public class StructureInfo {

    BlockPos origin;
    ResourceLocation id;
    int registryId;
    String commonName;

    public StructureInfo(BlockPos origin, ResourceLocation id, int rId, String commonName) {
        this.origin = origin;
        this.id = id;
        this.registryId = rId;
        this.commonName = commonName;
    }

    public StructureInfo(CompoundTag tag) {
        deserialize(tag);
    }


    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        if(origin != null) {
         String pos = HBUtil.BlockUtil.positionToString(origin);
            tag.putString("origin", pos);
        }
        if(id != null) tag.putString("id", id.toString());
        tag.putInt("registryId", registryId);
        if(commonName != null) tag.putString("commonName", commonName);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        if(tag.contains("origin")) {
            String pos = tag.getString("origin");
            origin = HBUtil.BlockUtil.stringToBlockPos (pos);
        }
        if(tag.contains("id")) {
            id = new ResourceLocation(tag.getString("id"));
        }

        registryId = tag.getInt("registryId");

        if(tag.contains("commonName")) {
            commonName = tag.getString("commonName");
        }
    }


    //* Statics

    public static StructureInfo of(Pair<BlockPos, Holder<Structure>> result, Registry<Structure> structureRegistry) {
        ResourceLocation id = structureRegistry.getKey(result.getSecond().value());
        int rId = structureRegistry.getId(result.getSecond().value());
        String commonName = id.getPath();
        return new StructureInfo(result.getFirst(), id, rId, commonName);
    }

    public static StructureInfo of(int rId, String blockPos, Registry<Structure> structureRegistry) {
        Structure struct = structureRegistry.byId(rId);
        ResourceLocation id = structureRegistry.getKey(struct);
        String commonName = id.getPath();
        BlockPos pos = HBUtil.BlockUtil.stringToBlockPos(blockPos);
        return new StructureInfo(pos, id, rId, commonName);
    }


}
