package com.holybuckets.foundation.modelInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface IManagedPlayer {

    default boolean isServerOnly(){return false;}

    default boolean isClientOnly() {return false;}

    /**
     * Initialize the ManagedPlayer data
     */
    boolean isInit(String subclass);

    /**
     * Get an existing instance from memory
     */
    IManagedPlayer getStaticInstance(Player player, String id);


    void handlePlayerJoin(Player player);

    default void handlePlayerLeave(Player player) {}

    default void handlePlayerRespawn(Player player){}
    
    /**
     * Called when player dies
     */
    default void handlePlayerDeath(Player player) {}

    /**
     * Called when player takes damage
     */
    default void handlePlayerDamage(Player player, float damageAmount) {}

    /**
     * Called when player falls
     */
    default void handlePlayerFall(Player player, float fallDistance, float damageMultiplier) {}

    /**
     * Called when player heals
     */
    default void handlePlayerHeal(Player player, float healAmount) {}

    /**
     * Called when player attacks an entity
     */
     default void handlePlayerAttack(Player player, Entity target){}

    /**
     * Called when player's dig speed is calculated
     */
    default void handlePlayerDigSpeed(Player player, float originalSpeed, Float newSpeed){}

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);

    void setId(String id);

    void setPlayer(Player player); 
}
