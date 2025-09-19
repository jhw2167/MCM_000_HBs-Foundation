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
        List<Integer> arr = new ArrayList<>(object.keyCodes);
        for (int i =0; i < ClientInputMessage.MAX_KEYS; i++) {
                buf.writeInt( (arr.size() <= i) ? -1 : arr.get(i) );
        }
        buf.writeUtf(object.side.name());
        return buf;
    }

    public static final ClientInputMessage decodeClientInput(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        InputType inputType = InputType.valueOf(buf.readUtf());

        Set<Integer> keyCodes = new HashSet<>();
        for (int i = 0; i < ClientInputMessage.MAX_KEYS; i++) {
            int code = buf.readInt();
            if (code != -1) {
                keyCodes.add(code);
            }
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


    public static final FriendlyByteBuf encodeControllerInput(ControllerInputMessage object, FriendlyByteBuf buf) {
        buf.writeInt(object.controllerInput);
        buf.writeInt(object.useDisplay);
        buf.writeBlockPos(object.pos);
        return buf;
    }

    public static final ControllerInputMessage decodeControllerInput(FriendlyByteBuf buf) {
        int controllerInput = buf.readInt();
        int useDisplay = buf.readInt();
        BlockPos pos = buf.readBlockPos();
        return new ControllerInputMessage(controllerInput, useDisplay, pos);
    }

    public static final FriendlyByteBuf encodeControllerDisplay(ControllerDisplayMessage object, FriendlyByteBuf buf) {
        buf.writeBlockPos(object.pos);
        buf.writeVarIntArray(object.displayData);
        return buf;
    }

    public static final ControllerDisplayMessage decodeControllerDisplay(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int[] displayData = buf.readVarIntArray();
        return new ControllerDisplayMessage(pos, displayData);
    }
}
