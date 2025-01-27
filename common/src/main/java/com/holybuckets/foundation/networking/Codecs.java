package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class Codecs {

    //ManagedChunk
    public static final FriendlyByteBuf encodeBlockStateUpdates(MessageBlockStateUpdates object, FriendlyByteBuf buf) {
        buf.writeUtf(HBUtil.LevelUtil.toLevelId(object.world));
        buf.writeUtf(HBUtil.BlockUtil.serializeBlockStatePairs(object.blocks));
        return buf;
    }

    public static final MessageBlockStateUpdates decodeBlockStateUpdates(FriendlyByteBuf buf) {
        LevelAccessor world = HBUtil.LevelUtil.toLevel( HBUtil.LevelUtil.LevelNameSpace.CLIENT, buf.readUtf());
        Map<BlockState, List<BlockPos>> blocks = HBUtil.BlockUtil.deserializeBlockStatePairs(buf.readUtf());
        return new MessageBlockStateUpdates(world, blocks);
    }


}
