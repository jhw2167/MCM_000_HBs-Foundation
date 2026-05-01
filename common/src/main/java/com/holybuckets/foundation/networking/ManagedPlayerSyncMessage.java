package com.holybuckets.foundation.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.holybuckets.foundation.FoundationInitializers.id;

/**
 * Clientbound packet: syncs the full ManagedPlayer NBT from server → client.
 */
public class ManagedPlayerSyncMessage implements CustomPacketPayload {

    public static final String LOCATION = "managed_player_sync";

    public static final CustomPacketPayload.Type<ManagedPlayerSyncMessage> TYPE =
        new CustomPacketPayload.Type<>(id(LOCATION));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManagedPlayerSyncMessage> STREAM_CODEC =
        CustomPacketPayload.codec(ManagedPlayerSyncMessage::write, ManagedPlayerSyncMessage::new);

    public final CompoundTag nbt;

    public ManagedPlayerSyncMessage(Comp