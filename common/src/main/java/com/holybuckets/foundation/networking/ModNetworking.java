package com.holybuckets.foundation.networking;

import net.blay09.mods.balm.network.BalmNetworking;
import com.holybuckets.foundation.networking.SimpleStringMessage.SimpleStringClientMessage;
import com.holybuckets.foundation.networking.SimpleStringMessage.SimpleStringServerMessage;

public class ModNetworking {

    public static void init(BalmNetworking networking) {
        Handlers.init();

        networking.registerServerboundPacket(
            ClientInputMessage.TYPE,
            ClientInputMessage.class,
            ClientInputMessage.STREAM_CODEC,
            Handlers::handleClientInput
        );

        networking.registerClientboundPacket(
            BlockStateUpdatesMessage.TYPE,
            BlockStateUpdatesMessage.class,
            BlockStateUpdatesMessage.STREAM_CODEC,
            Handlers::handleBlockStateUpdates
        );

        networking.registerClientboundPacket(
            SimpleStringClientMessage.TYPE,
            SimpleStringClientMessage.class,
            SimpleStringClientMessage.STREAM_CODEC,
            Handlers::handleSimpleString
        );

        networking.registerServerboundPacket(
            SimpleStringServerMessage.TYPE,
            SimpleStringServerMessage.class,
            SimpleStringServerMessage.STREAM_CODEC,
            Handlers::handleSimpleString
        );

        networking.registerClientboundPacket(
            StructureInfoMessage.TYPE,
            StructureInfoMessage.class,
            StructureInfoMessage.STREAM_CODEC,
            Handlers::handleStructureInfo
        );

        networking.registerClientboundPacket(
            ManagedPlayerSyncMessage.TYPE,
            ManagedPlayerSyncMessage.class,
            ManagedPlayerSyncMessage.STREAM_CODEC,
            ManagedPlayerSyncHandler::handle
        );
    }
}