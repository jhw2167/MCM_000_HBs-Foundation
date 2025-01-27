package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.nbt.CompoundTag;

public class Codecs {

    //ManagedChunk
    public static final String encodeBlockStateUpdates(MessageBlockStateUpdates object) {
        return HBUtil.BlockUtil.serializeBlockPairs(object.blocks);
    }

    public static final MessageBlockStateUpdates decodeBlockStateUpdates(String string) {
        return new MessageBlockStateUpdates(HBUtil.BlockUtil.deserializeBlockPairs(string));
    }

    

}
