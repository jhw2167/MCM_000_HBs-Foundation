package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

public class ItemEntityTickEvent {
    private final ItemEntity itemEntity;

    public ItemEntityTickEvent(ItemEntity itemEntity) {
        this.itemEntity = itemEntity;
    }

    public ItemEntity getItemEntity() {
        return itemEntity;
    }

    public boolean is(Item item) {
        return itemEntity.getItem().getItem() == item;
    }
}
