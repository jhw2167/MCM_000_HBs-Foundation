package com.holybuckets.foundation.structure;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.apache.commons.lang3.text.WordUtils;

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

    //** GETTERS
    public BlockPos getOrigin() {
        return origin;
    }

    public ResourceLocation getId() {
        return id;
    }

    public int getRegistryId() {
        return registryId;
    }

    public String getCommonName() {
        return commonName;
    }


    //** SERIALIZERS

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

    public static StructureInfo of(Holder<StructureType<?>> holder, BlockPos structurePos, Registry<StructureType<?>> structureRegistry) {
        ResourceLocation id = structureRegistry.getKey(holder.value());
        int rId = structureRegistry.getId(holder.value());
        String commonName = WordUtils.capitalizeFully(id.getPath().replace("_", " "));
        return new StructureInfo(structurePos, id, rId, commonName);
    }

    public static StructureInfo of(int rId, String blockPos, Registry<StructureType<?>> structureRegistry) {
        StructureType struct = structureRegistry.byId(rId);
        ResourceLocation id = structureRegistry.getKey(struct);
        String commonName = WordUtils.capitalizeFully(id.getPath().replace("_", " "));
        BlockPos pos = HBUtil.BlockUtil.stringToBlockPos(blockPos);
        return new StructureInfo(pos, id, rId, commonName);
    }


}
