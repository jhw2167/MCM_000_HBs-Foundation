package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.player.ManagedPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side handler for ManagedPlayerSyncMessage.
 * Applies incoming NBT to ManagedPlayer.CLIENT_PLAYER.
 */
public class ManagedPlayerSyncHandler {
    public static String CLASS_ID = "040";

    public static void handle(Player player, ManagedPlayerSyncMessage message)
    {
        ManagedPlayer mp = ManagedPlayer.CLIENT_PLAYER;
        if (mp == null) {
            LoggerBase.logError(null, "040020", "ManagedPlayerSyncHandler: CLIENT_PLAYER is null, cannot apply sync");
            return;
        }
        mp.syncClient(message.nbt);
    }
}