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

    /**
     * Called when player joins
     */
    void handlePlayerJoin(Player player);

    /**
     * Called when player leaves
     */  
    void handlePlayerLeave(Player player);
    
    /**
     * Called when player respawns
     */
    void handlePlayerRespawn(Player player);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);

    void setId(String id);

    void setPlayer(Player player); 
}
