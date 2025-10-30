package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.structure.StructureInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

    //SimpleStringMessage
    public static final FriendlyByteBuf encodeSimpleString(SimpleStringMessage object, FriendlyByteBuf buf) {
        buf.writeUUID(object.senderId);
        buf.writeUtf(object.content, SimpleStringMessage.MAX_SIZE);
        return buf;
    }

    public static final SimpleStringMessage decodeSimpleString(FriendlyByteBuf buf) {
        UUID senderId = buf.readUUID();
        String content = buf.readUtf(SimpleStringMessage.MAX_SIZE);
        return new SimpleStringMessage(senderId, content);
    }

    //StructureInfoMessage
    public static final FriendlyByteBuf encodeStructureInfo(StructureInfoMessage object, FriendlyByteBuf buf) {
        // Write sender ID (can be null for server messages)
        buf.writeBoolean(object.senderId != null);
        if (object.senderId != null) {
            buf.writeUUID(object.senderId);
        }
        
        // Write structure count and data
        buf.writeInt(object.structures.size());
        for (StructureInfo structure : object.structures) {
            CompoundTag tag = structure.serialize();
            buf.writeNbt(tag);
        }
        return buf;
    }

    public static final StructureInfoMessage decodeStructureInfo(FriendlyByteBuf buf) {
        // Read sender ID
        UUID senderId = null;
        if (buf.readBoolean()) {
            senderId = buf.readUUID();
        }
        
        // Read structures
        int count = buf.readInt();
        List<StructureInfo> structures = new ArrayList<>();
        for (int i = 0; i < count && i < StructureInfoMessage.MAX_STRUCTURES; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                StructureInfo info = StructureInfo.deserialize(tag);
                if (info != null) {
                    structures.add(info);
                }
            }
        }
        
        return new StructureInfoMessage(senderId, structures);
    }
}
