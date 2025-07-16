package com.holybuckets.foundation.modelInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IManagedPlayer {
    boolean isServerOnly();

    boolean isClientOnly();

    /**
     * Initialize the ManagedPlayer data
     */
    boolean isInit(String subclass);

    /**
     * Get an existing instance from memory
     */
    IManagedPlayer getStaticInstance(Player player, String id);

    void handlePlayerJoin(Player player);

    void handlePlayerLeave(Player player);

    void handlePlayerRespawn(Player player);
    
    /**
     * Called when player dies
     */
    void handlePlayerDeath(Player player);

    /**
     * Called when player attacks an entity
     */
    void handlePlayerAttack(Player player, Entity target);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);

    void setId(String id);

    void setPlayer(Player player); 
}
