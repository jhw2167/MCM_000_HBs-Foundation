package com.holybuckets.foundation.event.custom;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/**
 * Event fired when an anvil menu is updated with new items
 */
public class AnvilUpdateEvent {
    private final AnvilMenu anvilMenu;
    private final ItemStack leftItem;
    private final ItemStack rightItem;
    private ItemStack resultItem = null;
    private Integer cost = 0;

    public AnvilUpdateEvent(AnvilMenu anvilMenu, ItemStack leftItem, ItemStack rightItem) {
        this.anvilMenu = anvilMenu;
        this.leftItem = leftItem;
        this.rightItem = rightItem;
    }

    public AnvilUpdateEvent(ItemStack leftItem, ItemStack rightItem) {
        this.anvilMenu = null;
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

    public ItemStack getResultItem() {
        return resultItem;
    }

    public void setResultItem(ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    public Integer getResultCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AnvilUpdateEvent that = (AnvilUpdateEvent) obj;
        
        // Compare based on item types only
        return Objects.equals(getItemType(leftItem), getItemType(that.leftItem)) &&
               Objects.equals(getItemType(rightItem), getItemType(that.rightItem));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemType(leftItem), getItemType(rightItem));
    }

    private String getItemType(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return "empty";
        }
        return item.getItem().toString();
    }
}
