package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.model.ManagedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static java.lang.Thread.sleep;

/**
 * Description: MessageUpdateBlockStates
 * Packet data for block state updates from server to client
 */
public class BlockStateUpdatesMessage {

    public static final String LOCATION = "block_state_updates";
    private static final Integer BLOCKPOS_SIZE = 48;    //16 bytes per number x3 = 48 bytes
    LevelAccessor world;
    Map<BlockState, List<BlockPos>> blocks;

    BlockStateUpdatesMessage(LevelAccessor level, Map<BlockState, List<BlockPos>> blocks) {
        this.world = level;
        this.blocks = blocks;
    }

    public static void createAndFire(LevelAccessor world, Map<BlockState, List<BlockPos>> updates) {
        BlockStateUpdatesMessageHandler.createAndFire(world, updates);
    }
}
