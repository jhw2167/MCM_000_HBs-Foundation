package com.holybuckets.foundation.structure;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.apache.commons.lang3.text.WordUtils;

public class StructureInfo {

    BlockPos origin;
    Identifier id;
    int registryId;
    String commonName;
    Identifier structureLocation;

    public StructureInfo(BlockPos origin, Identifier id, int rId, String commonName, Identifier structureLocation) {
        this.origin = origin;
        this.id = id;
        this.registryId = rId;
        this.commonName = commonName;
        this.structureLocation = structureLocation;
    }

    public StructureInfo(CompoundTag tag) {
        deserialize(tag);
    }

    //** GETTERS
    public BlockPos getOrigin() {
        return origin;
    }

    public Identifier getId() {
        return id;
    }

    public int getRegistryId() {
        return registryId;
    }

    public String getCommonName() {
        return commonName;
    }

    public Identifier getStructureLocation() {
        return structureLocation;
    }

    //add equals method based on registry id and blockPos
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StructureInfo that = (StructureInfo) obj;
        return registryId == that.registryId && origin.equals(that.origin);
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
        if(structureLocation != null) tag.putString("structureLocation", structureLocation.toString());
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        if(tag.contains("origin")) {
            String pos = tag.getString("origin");
            origin = HBUtil.BlockUtil.stringToBlockPos (pos);
        }
        if(tag.contains("id")) {
            id = HBUtil.LOC(tag.getString("id"));
        }

        registryId = tag.getInt("registryId");

        if(tag.contains("commonName")) {
            commonName = tag.getString("commonName");
        }
        
        if(tag.contains("structureLocation")) {
            structureLocation = HBUtil.LOC(tag.getString("structureLocation"));
        }
    }


    //* Statics

    public static StructureInfo of(Holder<Structure> holder, BlockPos structurePos, Registry<Structure> structureRegistry, Identifier loc) {
        Identifier id = structureRegistry.getKey(holder.value());
        int rId = structureRegistry.getId(holder.value());
        String commonName = WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
        return new StructureInfo(structurePos, id, rId, commonName, loc);
    }

    public static StructureInfo of(int rId, String blockPos, Registry<Structure> structureRegistry, Identifier loc) {
        Structure struct = structureRegistry.byId(rId);
        Identifier id = structureRegistry.getKey(struct);
        String commonName = WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
        BlockPos pos = HBUtil.BlockUtil.stringToBlockPos(blockPos);
        return new StructureInfo(pos, id, rId, commonName, loc);
    }


}
