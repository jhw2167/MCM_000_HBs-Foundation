package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fired server side when a player claims an item from a crafting result slot, before the grid is consumed.
 * Changes made to the crafted stack carry into the player's inventory or cursor.
 */
public class PlayerCraftedEvent {

    /** Grid each player is crafting in, maintained by the crafting and inventory menu mixins */
    public static final Map<Player, CraftingContainer> CRAFTING_GRIDS = new ConcurrentHashMap<>();

    private final Player player;
    private final CraftingContainer craftSlots;
    private final ItemStack craftedItem;

    public PlayerCraftedEvent(Player player, @Nullable CraftingContainer craftSlots, ItemStack craftedItem) {
        this.player = player;
        this.craftSlots = craftSlots;
        this.craftedItem = craftedItem;
    }

    public Player getPlayer() { return player; }

    /** Grid the item was crafted from, still holding its ingredients. */
    public @Nullable CraftingContainer getCraftSlots() { return craftSlots; }

    public ItemStack getCraftedItem() { return craftedItem; }
}
