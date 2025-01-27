package com.holybuckets.foundation;

import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.networking.Codecs;
import com.holybuckets.foundation.networking.MessageBlockStateUpdates;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class FoundationInitializers {

    public static void init()
    {
        initEvents();
        initNetworking();
    }


    public static void initEvents()
    {
        ManagedChunk.init(EventRegistrar.getInstance());
        BalmEventRegister.registerEvents();
    }

    public static void initNetworking()
    {
        BalmNetworking networking = Balm.getNetworking();
        networking.registerClientboundPacket(id(MessageBlockStateUpdates.LOCATION), MessageBlockStateUpdates.class, Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates, MessageBlockStateUpdates::handle);
    }

    public static ResourceLocation id(String location) {
        return new ResourceLocation(Constants.MOD_ID, location);
    }
}
