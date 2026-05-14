package com.holybuckets.foundation.event.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import java.util.HashMap;

public class PlayerHasItemEvent {
    private final Player player;
    private final HashMap<Item, Integer> inventoryMap;

    public PlayerHasItemEvent(Player player, HashMap<Item, Integer> inventoryMap) {
        this.player = player;
        this.inventoryMap = inventoryMap;
    }

    public Player getPlayer() { return player; }
    public HashMap<Item, Integer> getInventoryMap() { return inventoryMap; }
}
