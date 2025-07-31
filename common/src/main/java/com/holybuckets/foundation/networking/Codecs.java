package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import static com.holybuckets.foundation.networking.ClientInputMessage.InputType;
import static com.holybuckets.foundation.HBUtil.LevelUtil.LevelNameSpace;

import java.util.*;

public class Codecs {
    
    public static final FriendlyByteBuf encodeClientInput(ClientInputMessage object, FriendlyByteBuf buf) {
        buf.writeUUID(object.playerId);
        buf.writeUtf(InputType.valueOf(object.inputType.name()).name());
        // Write the set of key codes
        buf.writeInt(object.keyCodes.size());
        for (Integer keyCode : object.keyCodes) {
            buf.writeInt(keyCode);
        }
        buf.writeUtf(object.side.name());
        return buf;
    }

    public static final ClientInputMessage decodeClientInput(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        InputType inputType = InputType.valueOf(buf.readUtf());
        // Read the set of key codes
        int keyCount = buf.readInt();
        Set<Integer> keyCodes = new HashSet<>();
        for (int i = 0; i < keyCount; i++) {
            keyCodes.add(buf.readInt());
        }
        LevelNameSpace side = LevelNameSpace.valueOf(buf.readUtf());
        return new ClientInputMessage(playerId, inputType, keyCodes, side);
    }

    //ManagedChunk
    public static final FriendlyByteBuf encodeBlockStateUpdates(BlockStateUpdatesMessage object, FriendlyByteBuf buf) {
        buf.writeUtf(HBUtil.LevelUtil.toLevelId(object.world));
        buf.writeUtf(HBUtil.BlockUtil.serializeBlockStatePairs(object.blockStates));
        return buf;
    }

    public static final BlockStateUpdatesMessage decodeBlockStateUpdates(FriendlyByteBuf buf) {
        LevelAccessor world = HBUtil.LevelUtil.toLevel( HBUtil.LevelUtil.LevelNameSpace.CLIENT, buf.readUtf());
        Map<BlockState, List<BlockPos>> blocks = HBUtil.BlockUtil.deserializeBlockStatePairs(buf.readUtf());
        return new BlockStateUpdatesMessage(world, blocks);
    }


}
