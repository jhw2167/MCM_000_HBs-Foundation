package com.holybuckets.foundation.networking;

import net.blay09.mods.balm.api.network.BalmNetworking;
import static com.holybuckets.foundation.FoundationInitializers.id;

import com.holybuckets.foundation.networking.SimpleStringMessage.SimpleStringClientMessage;
import com.holybuckets.foundation.networking.SimpleStringMessage.SimpleStringServerMessage;

public class ModNetworking {

    public static void init(BalmNetworking networking) {
        Handlers.init();
        networking.registerClientboundPacket(id(BlockStateUpdatesMessage.LOCATION), BlockStateUpdatesMessage.class, Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates, Handlers::handleBlockStateUpdates);

        networking.registerServerboundPacket(id(ClientInputMessage.LOCATION), ClientInputMessage.class, Codecs::encodeClientInput, Codecs::decodeClientInput, Handlers::handleClientInput);

        networking.registerClientboundPacket(id(SimpleStringMessage.LOCATION+"_client"), SimpleStringClientMessage.class, Codecs::encodeSimpleString, Codecs::decodeSimpleClientString, Handlers::handleSimpleString);
        networking.registerServerboundPacket(id(SimpleStringMessage.LOCATION+"_server"), SimpleStringServerMessage.class, Codecs::encodeSimpleString, Codecs::decodeSimpleServerString, Handlers::handleSimpleString);

        networking.registerClientboundPacket(id(StructureInfoMessage.LOCATION), StructureInfoMessage.class, Codecs::encodeStructureInfo, Codecs::decodeStructureInfo, Handlers::handleStructureInfo);

        networking.registerClientboundPacket(
            id(ManagedPlayerSyncMessage.LOCATION), ManagedPlayerSyncMessage.class, Codecs::encode, Codecs::decode, ManagedPlayerSyncHandler::handle
        );
    }
}
