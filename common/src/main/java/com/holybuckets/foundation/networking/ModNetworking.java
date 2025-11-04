package com.holybuckets.foundation.networking;

import net.blay09.mods.balm.api.network.BalmNetworking;
import static com.holybuckets.foundation.FoundationInitializers.id;

public class ModNetworking {

    public static void init(BalmNetworking networking) {
        Handlers.init();
        networking.registerClientboundPacket(id(BlockStateUpdatesMessage.LOCATION), BlockStateUpdatesMessage.class, Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates, Handlers::handleBlockStateUpdates);

        networking.registerServerboundPacket(id(ClientInputMessage.LOCATION), ClientInputMessage.class, Codecs::encodeClientInput, Codecs::decodeClientInput, Handlers::handleClientInput);

        networking.registerClientboundPacket(id(SimpleStringMessage.LOCATION), SimpleStringMessage.class, Codecs::encodeSimpleString, Codecs::decodeSimpleString, Handlers::handleSimpleString);
        networking.registerServerboundPacket(id(SimpleStringMessage.LOCATION), SimpleStringMessage.class, Codecs::encodeSimpleString, Codecs::decodeSimpleString, Handlers::handleSimpleString);

        networking.registerClientboundPacket(id(StructureInfoMessage.LOCATION), StructureInfoMessage.class, Codecs::encodeStructureInfo, Codecs::decodeStructureInfo, Handlers::handleStructureInfo);
    }
}
