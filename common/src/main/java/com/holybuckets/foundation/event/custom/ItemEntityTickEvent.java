package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.item.ItemEntity;

public class ItemEntityTickEvent {
    private final ItemEntity itemEntity;

    public ItemEntityTickEvent(ItemEntity itemEntity) {
        this.itemEntity = itemEntity;
    }

    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
