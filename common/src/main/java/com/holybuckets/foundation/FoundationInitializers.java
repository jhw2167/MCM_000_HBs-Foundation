package com.holybuckets.foundation;

import com.holybuckets.foundation.biome.BiomeManager;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.block.entity.ModBlockEntities;
import com.holybuckets.foundation.command.CommandList;
import com.holybuckets.foundation.config.ModConfig;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.console.Messager;
import com.holybuckets.foundation.core.*;
import com.holybuckets.foundation.enchantment.ModEnchantments;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.item.ModItems;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.networking.*;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.sample.SamplePlayerData;
import com.holybuckets.foundation.structure.StructureManager;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

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
        ModEnchantments.register();
    }


    /**
     * Description: Initialize common HB utilities that must support all mods prior to mod initialization
     */
    public static synchronized void commonInitialize()
    {
        if(commonIninitialized) return;
        commonIninitialized = true;
        CommonClass.MESSAGER = Messager.getInstance();

        EventRegistrar.init();
        HBUtil.init(EventRegistrar.getInstance());
        GeneralConfig.init(EventRegistrar.getInstance());
        ModConfig.init(EventRegistrar.getInstance());

        ModItems.commonInitialize(Balm.getItems());
    }

    private static void initConfig()
    {
        PerformanceImpactConfig.initialize();
    }

    private static void initEvents()
    {
        EventRegistrar reg = EventRegistrar.getInstance();
        CommandList.init(reg);
        SamplePlayerData.init(reg);
        ManagedChunk.init(reg);
        ManagedPlayer.init(reg);

        StructureManager.init(reg);
        BiomeManager.init(reg);
        //ChunkExplorerManager.init(reg);
        //ClientInput.init(reg);

        //Core Operations
        EssenceCauldronManager.init(reg);
        CustomRecipes.init(EventRegistrar.getInstance());
    }

    private static void initCommands()
    {
        CommandList.register();
        BalmEventRegister.registerCommands();
    }


    private static void initNetworking()
    {
        BalmNetworking networking = Balm.getNetworking();
        ModNetworking.init(networking);
    }

    private static void initBlocks() {
        ModBlocks.initialize(Balm.getBlocks());
        ModBlockEntities.initialize(Balm.getBlockEntities());
    }

    private static void initItems() {
        ModItems.initialize(Balm.getItems());
    }

    public static ResourceLocation id(String location) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, location);
    }

}
