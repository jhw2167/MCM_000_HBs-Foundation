package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.structure.StructureInfo;
import com.holybuckets.foundation.structure.StructureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.holybuckets.foundation.FoundationInitializers.id;

/**
 * Description: StructureInfoMessage
 * A message type for sending structure information data
 */
public class StructureInfoMessage implements CustomPacketPayload {

    public static final String LOCATION = "structure_info";
    public static final int MAX_STRUCTURES = 16;

    public static final CustomPacketPayload.Type<StructureInfoMessage> TYPE =
        new CustomPacketPayload.Type<>(id(LOCATION));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureInfoMessage> STREAM_CODEC =
        CustomPacketPayload.codec(Codecs::encodeStructureInfo, Codecs::decodeStructureInfo);

    public final UUID senderId;
    public final List<StructureInfo> structures;

    StructureInfoMessage(UUID senderId, List<StructureInfo> structures) {
        this.senderId = senderId;
        this.structures = structures != null && structures.size() > MAX_STRUCTURES
            ? structures.subList(0, MAX_STRUCTURES)
            : (structures != null ? structures : List.of());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void createAndFire(Player p, List<StructureInfo> structures) {
        HBUtil.NetworkUtil.serverSendToPlayer(p, new StructureInfoMessage(p.getUUID(), structures));
    }

    public static class StructureInfoMessageHandler {

        public static String CLASS_ID = "017";

        public static void handle(Player player, StructureInfoMessage message) {
            StructureManager.handleStructureInfoFromServer(player, message);
        }
    }
}
