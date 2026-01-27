package com.holybuckets.foundation.event.custom;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Event fired when an anvil menu is updated with new items
 */
public class AnvilUpdateEvent {
    private final AnvilMenu anvilMenu;
    private final ItemStack leftItem;
    private final ItemStack rightItem;

    public AnvilUpdateEvent(AnvilMenu anvilMenu, ItemStack leftItem, ItemStack rightItem) {
        this.anvilMenu = anvilMenu;
        this.leftItem = leftItem;
        this.rightItem = rightItem;
    }

    public AnvilMenu getAnvilMenu() {
        return anvilMenu;
    }

    public ItemStack getLeftItem() {
        return leftItem;
    }

    public ItemStack getRightItem() {
        return rightItem;
    }
}
