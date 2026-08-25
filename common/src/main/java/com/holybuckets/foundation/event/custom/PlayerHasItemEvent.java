package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;

public class PlayerHasItemEvent {
    private final Player player;
    private final HashMap<Item, Integer> inventoryMap;
    private final ItemStack stack;
    private final int slot;

    public PlayerHasItemEvent(Player player, HashMap<ItemStack, Integer> inventoryMap) {
        this(player, null, -1, inventoryMap);
    }

    public PlayerHasItemEvent(Player player, ItemStack stack, int slot, HashMap<ItemStack, Integer> inventoryMap) {
        this.player = player;
        this.stack = stack;
        this.slot = slot;
        var itemMap = new HashMap<Item, Integer>();
        for (var entry : inventoryMap.entrySet()) {
            itemMap.put(entry.getKey().getItem(), entry.getValue());
        }
        this.inventoryMap = itemMap;
    }

    public Player getPlayer() { return player; }
    public ItemStack getItemStack() { return stack; }
    public int getSlot() { return slot; }
    public HashMap<Item, Integer> getInventoryMap() { return inventoryMap; }
}
