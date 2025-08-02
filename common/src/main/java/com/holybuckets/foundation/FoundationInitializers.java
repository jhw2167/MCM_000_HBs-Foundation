package com.holybuckets.foundation;

import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.block.entity.ModBlockEntities;
import com.holybuckets.foundation.client.ModRenderers;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.item.ModItems;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.holybuckets.foundation.networking.Codecs;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
import com.holybuckets.foundation.networking.Handlers;
import com.holybuckets.foundation.player.ManagedPlayer;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class FoundationInitializers {

    private static boolean commonIninitialized = false;

    static void init()
    {
        commonInitialize();

        initEvents();
        initCommands();
        initConfig();
        initNetworking();

        initBlocks();
        initItems();
    }


    /**
     * Description: Initialize common HB utilities that must support all mods prior to mod initialization
     */
    public static synchronized void commonInitialize()
    {
        if(commonIninitialized) return;
        commonIninitialized = true;

        EventRegistrar.init();
        HBUtil.NetworkUtil.init(EventRegistrar.getInstance());
        GeneralConfig.init(EventRegistrar.getInstance());
        ModItems.commonInitialize(Balm.getItems());
    }

    private static void initConfig()
    {
        PerformanceImpactConfig.initialize();
    }

    private static void initEvents()
    {
        EventRegistrar reg = EventRegistrar.getInstance();
        //SamplePlayerData.init(reg);
        ManagedChunk.init(reg);
        ManagedPlayer.init(reg);
        //ClientInput.init(reg);

        BalmEventRegister.registerEvents();
    }

    private static void initCommands()
    {
        BalmEventRegister.registerCommands();
    }


    private static void initNetworking()
    {
        BalmNetworking networking = Balm.getNetworking();
        Handlers.init();
        networking.registerClientboundPacket(id(BlockStateUpdatesMessage.LOCATION), BlockStateUpdatesMessage.class, Codecs::encodeBlockStateUpdates, Codecs::decodeBlockStateUpdates, Handlers::handleBlockStateUpdates);
        networking.registerServerboundPacket(id(ClientInputMessage.LOCATION), ClientInputMessage.class, Codecs::encodeClientInput, Codecs::decodeClientInput, Handlers::handleClientInput);
    }

    private static void initBlocks() {
        ModBlocks.initialize(Balm.getBlocks());
        ModBlockEntities.initialize(Balm.getBlockEntities());
    }

    private static void initItems() {
        ModItems.initialize(Balm.getItems());
    }


    //** CLIENT INITIALIZERS **//
    public static void initRenderers() {
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }



    private static ResourceLocation id(String location) {
        return new ResourceLocation(Constants.MOD_ID, location);
    }
}
