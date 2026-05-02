package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static com.holybuckets.foundation.FoundationInitializers.id;

/**
 * Description: MessageUpdateBlockStates
 * Packet data for block state updates from server to client
 */
public class BlockStateUpdatesMessage implements CustomPacketPayload {

    public static final String LOCATION = "block_state_updates";

    public static final CustomPacketPayload.Type<BlockStateUpdatesMessage> TYPE =
        new CustomPacketPayload.Type<>(id(LOCATION));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockStateUpdatesMessage> STREAM_CODEC =
        CustomPacketPayload.codec(Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates);

    LevelAccessor world;
    Map<BlockState, List<BlockPos>> blockStates;

    BlockStateUpdatesMessage(LevelAccessor level, Map<BlockState, List<BlockPos>> blocks) {
        this.world = level;
        this.blockStates = blocks;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void createAndFire(LevelAccessor world, Map<BlockState, List<BlockPos>> updates) {
        BlockStateUpdatesMessageHandler.createAndFire(world, updates);
    }
}
