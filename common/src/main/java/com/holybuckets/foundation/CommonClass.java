package com.holybuckets.foundation;

import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.platform.Services;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;


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
        GeneralConfig.init(EventRegistrar.getInstance());
        //sample(EventRegistrar.getInstance());

        FoundationInitializers.init();

        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample(EventRegistrar reg)
    {
        //reg.registerOnChunkLoad(CommonClass::onChunkLoad);
        reg.registerOnLevelLoad(CommonClass::onLevelLoad);
        reg.registerOnPlayerLoad(CommonClass::onPlayerLoad);
    }

    private static void onPlayerLoad(PlayerLoginEvent event)
    {
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();

        for(int x = -4; x < 4; x++)
        {
            for(int z = -4; z < 4; z++)
            {
                blocks.add(Pair.of(GOLD, new BlockPos(x*16, 128, z*16)));
            }
        }

        ManagedChunk.updateChunkBlockStates( event.getPlayer().level(), blocks);
    }


    public static void onChunkLoad(ChunkLoadingEvent event)
    {
        Constants.LOG.info("Chunk loaded: " + event.getChunkPos());
    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}