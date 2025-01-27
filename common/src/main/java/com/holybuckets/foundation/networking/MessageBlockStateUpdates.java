package com.holybuckets.foundation.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * Description: MessageUpdateBlockStates
 * Packet data for block state updates from server to client
 */
public class MessageBlockStateUpdates {

    public static final String LOCATION = "blockStateUpdates";
    Map<Block, List<BlockPos>> blocks;

    public MessageBlockStateUpdates(Map<Block, List<BlockPos>> blocks) {
        this.blocks = blocks;
    }

}
