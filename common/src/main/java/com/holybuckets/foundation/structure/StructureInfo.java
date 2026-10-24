package com.holybuckets.foundation.structure;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.apache.commons.lang3.text.WordUtils;

public class StructureInfo {

    BlockPos origin;
    ResourceLocation id;
    int registryId;
    String commonName;
    ResourceLocation structureLocation;
    int currentStage = -1;
    ResourceLocation[] structureStages = new ResourceLocation[0];
    String structureConcept = null;

    public StructureInfo(BlockPos origin, ResourceLocation id, int rId, String commonName, ResourceLocation structureLocation) {
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

    public ResourceLocation getId() {
        return id;
    }

    public int getRegistryId() {
        return registryId;
    }

    public String getCommonName() {
        ResourceLocation loc = getCurrentStructureLocation();
        if (loc == null) return commonName;
        return WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
    }

    public ResourceLocation getStructureLocation() {
        return structureLocation;
    }

    public ResourceLocation getCurrentStructureLocation() {
        if (currentStage >= 0 && currentStage < structureStages.length && structureStages[currentStage] != null)
            return structureStages[currentStage];
        return structureLocation;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public ResourceLocation[] getStructureStages() {
        return structureStages;
    }

    public String getStructureConcept() {
        return structureConcept;
    }

    public void setStructureConcept(String structureConcept) {
        this.structureConcept = structureConcept;
    }

    public void setStructureStages(ResourceLocation[] structureStages) {
        this.structureStages = (structureStages == null) ? new ResourceLocation[0] : structureStages;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public void setAsUpgradableStructure(int currentStage, ResourceLocation[] structureStages) {
        this.currentStage = currentStage;
        setStructureStages(structureStages);
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
            tag.putLong("chunkPos", HBUtil.ChunkUtil.getChunkPos1DMap(new ChunkPos(origin)));
            tag.putInt("y", origin.getY());
        }
        if(id != null) tag.putString("id", id.toString());
        tag.putInt("registryId", registryId);
        if(commonName != null) tag.putString("commonName", commonName);
        if(structureLocation != null) tag.putString("structureLocation", structureLocation.toString());
        tag.putInt("currentStage", currentStage);
        if(structureConcept != null) tag.putString("structureConcept", structureConcept);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        if(tag.contains("chunkPos")) {
            origin = HBUtil.ChunkUtil.getWorldPosFrom1DMap(tag.getLong("chunkPos"), tag.getInt("y"));
        } else if(tag.contains("origin")) {
            origin = HBUtil.BlockUtil.stringToBlockPos(tag.getString("origin"));
        }
        if(tag.contains("id")) {
            id = new ResourceLocation(tag.getString("id"));
        }

        registryId = tag.getInt("registryId");

        if(tag.contains("commonName")) {
            commonName = tag.getString("commonName");
        }
        
        if(tag.contains("structureLocation")) {
            structureLocation = new ResourceLocation(tag.getString("structureLocation"));
        }

        currentStage = tag.contains("currentStage") ? tag.getInt("currentStage") : -1;
        structureConcept = tag.contains("structureConcept") ? tag.getString("structureConcept") : null;
    }


    //* Statics

    public static StructureInfo of(Holder<Structure> holder, BlockPos structurePos, Registry<Structure> structureRegistry, ResourceLocation loc) {
        ResourceLocation id = structureRegistry.getKey(holder.value());
        int rId = structureRegistry.getId(holder.value());
        String commonName = WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
        return new StructureInfo(structurePos, id, rId, commonName, loc);
    }

    public static StructureInfo of(int rId, String blockPos, Registry<Structure> structureRegistry, ResourceLocation loc) {
        Structure struct = structureRegistry.byId(rId);
        ResourceLocation id = structureRegistry.getKey(struct);
        String commonName = WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
        BlockPos pos = HBUtil.BlockUtil.stringToBlockPos(blockPos);
        return new StructureInfo(pos, id, rId, commonName, loc);
    }

    public static StructureInfo of(int rId, long chunkPos, int y, Registry<Structure> structureRegistry, ResourceLocation loc) {
        Structure struct = structureRegistry.byId(rId);
        ResourceLocation id = structureRegistry.getKey(struct);
        String commonName = WordUtils.capitalizeFully(loc.getPath().replace("_", " "));
        BlockPos pos = HBUtil.ChunkUtil.getWorldPosFrom1DMap(chunkPos, y);
        return new StructureInfo(pos, id, rId, commonName, loc);
    }


}
