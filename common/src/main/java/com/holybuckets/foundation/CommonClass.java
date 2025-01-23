package com.holybuckets.foundation;

import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.platform.Services;
import net.blay09.mods.balm.api.event.ChunkEvent;
import net.blay09.mods.balm.api.event.LevelEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;


public class CommonClass {

    public static boolean isInitialized = false;
    public static void init()
    {
        //Initialize Foundations
        if (isInitialized)
            return;

        Constants.LOG.info("Loaded {} mod on {}! we are currently in a {} environment!", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {

            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        EventRegistrar.init();
        EventRegistrar reg = EventRegistrar.getInstance();

        GeneralConfig.init(reg);
        ManagedChunk.init(reg);

        //reg.registerOnChunkLoad(CommonClass::onChunkLoad);
        reg.registerOnLevelLoad(CommonClass::onLevelLoad);

        //Register all events
        BalmEventRegister.registerEvents();

        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }

    public static void onChunkLoad(ChunkEvent event)
    {
        Constants.LOG.info("Chunk loaded: " + event.getChunkPos());
    }

    public static void onLevelLoad(LevelEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}