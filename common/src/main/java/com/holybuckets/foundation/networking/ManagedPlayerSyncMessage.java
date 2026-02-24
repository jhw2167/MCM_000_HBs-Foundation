package com.holybuckets.foundation.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Clientbound packet: syncs the full ManagedPlayer NBT from server → client.
 */
public class ManagedPlayerSyncMessage {

    public static final String LOCATION = "managed_player_sync";

    public final CompoundTag nbt;

    public ManagedPlayerSyncMessage(CompoundTag nbt) {
        this.nbt = nbt != null ? nbt : new CompoundTag();
    }

}