package com.holybuckets.foundation.block.entity;

import net.blay09.mods.balm.common.BalmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * A block that indicates the bottom of a portal
 */
public class SimpleBlockEntity extends BalmBlockEntity {

    private Map<String,String> data;
    public SimpleBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.simpleBlockEntity.get(), blockPos, blockState);
        data = new HashMap<>();
    }

    // Getters/setters for use in menus or logic
    public String getProperty(String key) {
        return data.get(key);
    }

    public void setProperty(String key, String value) {
        data.put(key, value);
    }


    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("data")) {
            CompoundTag dataTag = nbt.getCompound("data");
            for (String key : dataTag.getAllKeys()) {
                data.put(key, dataTag.getString(key));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag initialData) {
        super.saveAdditional(initialData);
        CompoundTag dataTag = new CompoundTag();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            dataTag.putString(entry.getKey(), entry.getValue());
        }
        initialData.put("data", dataTag);
        this.setChanged();
    }
}
