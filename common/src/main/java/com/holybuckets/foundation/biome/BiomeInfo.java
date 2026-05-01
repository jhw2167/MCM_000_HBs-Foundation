package com.holybuckets.foundation.biome;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.text.WordUtils;

public class BiomeInfo {

    BlockPos samplePos;   // one representative position inside the biome (chunk center)
    ResourceLocation id;
    int registryId;
    String commonName;

    public BiomeInfo(BlockPos samplePos, ResourceLocation id, int registryId, String commonName) {
        this.samplePos = samplePos;
        this.id = id;
        this.registryId = registryId;
        this.commonName = commonName;
    }

    public BiomeInfo(CompoundTag tag) {
        deserialize(tag);
    }

    //** GETTERS

    public BlockPos getSamplePos() {
        return samplePos;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BiomeInfo that = (BiomeInfo) obj;
        return registryId == that.registryId && samplePos.equals(that.samplePos);
    }

    @Override
    public int hashCode() {
        return 31 * registryId + samplePos.hashCode();
    }

    //** SERIALIZERS

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        if (samplePos != null) {
            tag.putString("samplePos", HBUtil.BlockUtil.positionToString(samplePos));
        }
        if (id != null) tag.putString("id", id.toString());
        tag.putInt("registryId", registryId);
        if (commonName != null) tag.putString("commonName", commonName);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        if (tag.contains("samplePos")) {
            samplePos = HBUtil.BlockUtil.stringToBlockPos(tag.getString("samplePos"));
        }
        if (tag.contains("id")) {
            id = ResourceLocation.tryParse(tag.getString("id"));
        }
        registryId = tag.getInt("registryId");
        if (tag.contains("commonName")) {
            commonName = tag.getString("commonName");
        }
    }

    //** STATICS

    public static BiomeInfo of(Holder<Biome> holder, BlockPos samplePos, Registry<Biome> biomeRegistry) {
        ResourceLocation id = holder.unwrapKey()
            .map(key -> key.location())
            .orElse(biomeRegistry.getKey(holder.value()));
        int rId = biomeRegistry.getId(holder.value());
        String commonName = id != null
            ? WordUtils.capitalizeFully(id.getPath().replace("_", " "))
            : "Unknown";
        return new BiomeInfo(samplePos, id, rId, commonName);
    }

    public static BiomeInfo of(int rId, String blockPos, Registry<Biome> biomeRegistry) {
        Biome biome = biomeRegistry.byId(rId);
        ResourceLocation id = biomeRegistry.getKey(biome);
        String commonName = id != null
            ? WordUtils.capitalizeFully(id.getPath().replace("_", " "))
            : "Unknown";
        BlockPos pos = HBUtil.BlockUtil.stringToBlockPos(blockPos);
        return new BiomeInfo(pos, id, rId, commonName);
    }
}