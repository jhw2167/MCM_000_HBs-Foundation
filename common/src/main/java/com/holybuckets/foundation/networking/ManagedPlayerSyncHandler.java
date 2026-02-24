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

    public static void handle(Player player, ManagedPlayerSyncMessage message)
    {
        ManagedPlayer mp = ManagedPlayer.CLIENT_PLAYER;
        if (mp == null) {
            LoggerBase.logError(null, "004020",
                "ManagedPlayerSyncHandler: CLIENT_PLAYER is null, cannot apply sync");
            return;
        }

        CompoundTag nbt = message.nbt;
        if (nbt == null || nbt.isEmpty()) return;

        try {
            mp.deserializeNBT(nbt);
        } catch (Exception e) {
            LoggerBase.logError(null, "004021",
                "ManagedPlayerSyncHandler: error applying NBT sync: " + e.getMessage());
        }
    }
}