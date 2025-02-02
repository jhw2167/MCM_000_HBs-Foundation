package com.holybuckets.foundation;

import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.config.PerformanceImpactConfigData;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.networking.Codecs;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
import com.holybuckets.foundation.networking.Handlers;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class FoundationInitializers {

    static void init()
    {
        commonInitialize();
        
        initEvents();
        initConfig();
        initNetworking();
    }

    /**
     * Description: Initialize common HB utilities that much support all mods prior to mod initialization
     */
    public static void commonInitialize()
    {
        if(CommonClass.isInitialized) return;

        EventRegistrar.init();
        HBUtil.NetworkUtil.init(EventRegistrar.getInstance());
        GeneralConfig.init(EventRegistrar.getInstance());
    }


    private static void initEvents()
    {
        ManagedChunk.init(EventRegistrar.getInstance());
        BalmEventRegister.registerEvents();
    }

    private static void initConfig()
    {
        PerformanceImpactConfig.initialize();
    }

    private static void initNetworking()
    {
        BalmNetworking networking = Balm.getNetworking();
        Handlers.init();
        networking.registerClientboundPacket(id(BlockStateUpdatesMessage.LOCATION), BlockStateUpdatesMessage.class, Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates, Handlers::handleBlockStateUpdates);
    }

    private static ResourceLocation id(String location) {
        return new ResourceLocation(Constants.MOD_ID, location);
    }
}
