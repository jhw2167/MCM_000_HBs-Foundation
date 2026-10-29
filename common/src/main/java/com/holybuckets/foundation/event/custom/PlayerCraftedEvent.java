package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Fired server side when a player claims an item from a crafting result slot, before the grid is consumed.
 * Changes made to the crafted stack carry into the player's inventory or cursor.
 */
public class PlayerCraftedEvent {

    private final Player player;
    private final CraftingContainer craftSlots;
    private final ItemStack craftedItem;

    public PlayerCraftedEvent(Player player, CraftingContainer craftSlots, ItemStack craftedItem) {
        this.player = player;
        this.craftSlots = craftSlots;
        this.craftedItem = craftedItem;
    }

    public Player getPlayer() { return player; }

    /** Grid the item was crafted from, still holding its ingredients. Do not mutate. */
    public CraftingContainer getCraftSlots() { return craftSlots; }

    /** The stack the player is claiming; alter it in place */
    public ItemStack getCraftedItem() { return craftedItem; }
}
