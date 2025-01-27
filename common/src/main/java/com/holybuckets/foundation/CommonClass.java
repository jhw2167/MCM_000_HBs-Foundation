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
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
        sample(EventRegistrar.getInstance());

        FoundationInitializers.init();

        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample(EventRegistrar reg)
    {
        reg.registerOnChunkLoad(CommonClass::onChunkLoad);
        //reg.registerOnLevelLoad(CommonClass::onLevelLoad);
        //reg.registerOnPlayerLoad(CommonClass::onPlayerLoad);
    }

    private static void onPlayerLoad(PlayerLoginEvent event) {
        Constants.LOG.info("Player loaded: " + event.getPlayer().getGameProfile().getName());
    }


    public static void onChunkLoad(ChunkLoadingEvent.Load event) {
        //Constants.LOG.info("Chunk loaded: " + event.getChunkPos());
        new Thread(() -> threadAddChunkBlock(event)).start();
    }

    public static volatile int threadsRunning = 0;
    public static void threadAddChunkBlock(ChunkLoadingEvent.Load event)
    {
        if( event.getLevel().isClientSide() ) return;

        threadsRunning++;

        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();
        ChunkAccess c = event.getChunk();
        BlockPos p = c.getPos().getWorldPosition();

        final int RANGE = 16;
        final int Y = new Random(p.getX()*p.getZ()).nextInt(128);
        BlockPos pos = null;
        for(int x = 0; x < RANGE; x++) {
            for(int z = 0; z < RANGE; z++) {
                pos = new BlockPos(p.getX()+x, Y, p.getZ()+z);
                blocks.add(Pair.of(GOLD, new BlockPos(p.getX()+x, Y, p.getZ()+z )));
            }
        }
        LoggerBase.logInfo(null, "000000", "1) ADD: " + pos);

        int startTime = (int) System.currentTimeMillis();
        while( !ManagedChunk.updateChunkBlockStates( (ServerLevel) event.getLevel(), blocks) ) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int endTime = (int) System.currentTimeMillis();
        LoggerBase.logInfo(null, "000000", "2) REMOVE: " + pos + " TIME: " + (endTime - startTime) + "ms " +
        " THREADS RUNNING: " + threadsRunning);
        threadsRunning--;

    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}