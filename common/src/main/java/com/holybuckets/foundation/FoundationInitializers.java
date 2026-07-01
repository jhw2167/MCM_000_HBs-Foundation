package com.holybuckets.foundation;

import com.holybuckets.foundation.biome.BiomeManager;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.block.entity.ModBlockEntities;
import com.holybuckets.foundation.command.CommandList;
import com.holybuckets.foundation.config.ModConfig;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.console.Messager;
import com.holybuckets.foundation.core.*;
import com.holybuckets.foundation.datacomponent.ModDataComponents;
import com.holybuckets.foundation.enchantment.ModEnchantments;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.item.ModItems;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.networking.*;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.sample.SamplePlayerData;
import com.holybuckets.foundation.structure.StructureManager;
import com.holybuckets.foundation.util.ModContext;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class FoundationInitializers {

    private static boolean commonIninitialized = false;

    static void init()
    {
        commonInitialize();
        checkInitializationErrors();

        initEvents();
        initCommands();
        initConfig();
        initNetworking();

        initBlocks();
        initItems();
        ModEnchantments.register();
        ModDataComponents.register(Balm.getRuntime().registrar());
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
        com.holybuckets.foundation.core.MovingWaypoint.init(reg);
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
        return HBUtil.LOC(Constants.MOD_ID, location);
    }


    //version verify, make sure if the user is running mod: hbs_ore_cluster_and_regen BELOW 1.5, they need to update
    //balm has a version API to verify


    public static final String ORE_CLUSTER_MOD_ID = "hbs_ore_cluster_and_regen";
    public static final String MIN_COMPATIBLE_VERSION = "1.5";
    public static final String SATELLITE_MOD_ID = "hbs_satellites";

    public static void checkInitializationErrors()
    {
        validateModVersions();
    }

    private static void validateModVersions()
    {

        var optVers = ModContext.getInstance().getVersion(ORE_CLUSTER_MOD_ID);
        if(optVers.isPresent())
        {
            String version = optVers.get();
            int[] parsed = parseVersion(version);
            if(parsed[0] <= 1 && parsed[1] < 5) {
                LoggerBase.logError(null, "999999", ORE_CLUSTER_VALIDATE_ERROR);
                throw new RuntimeException(ORE_CLUSTER_VALIDATE_ERROR);
            }
        }

    }

    private static int[] parseVersion(String version) {
        // Strip any build metadata or pre-release suffix (e.g. "1.5.2-forge" -> "1.5.2")
        String clean = version.split("[-+]")[0];
        String[] parts = clean.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }


    private static String ORE_CLUSTER_VALIDATE_ERROR = " This version of hbs_foundation is INCOMPATIBLE with " +
     "hbs_ore_cluster_and_regen versions below v1.5. Please update hbs_ore_cluster_and_regen " +
      "to version 1.5 or higher";

    private static String SATELLITES_VALIDATE_ERROR = " This version of hbs_foundation is INCOMPATIBLE with " +
        "hbs_satellites versions below v1.5. Please update hbs_ore_cluster_and_regen " +
        "to version 1.5 or higher";


}
