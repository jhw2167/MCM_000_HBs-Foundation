package com.holybuckets.foundation;

import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtilityAccessor;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;


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


    //Create a threadpool to add blocks to a chunk, max 16 threads, store in queue
    public static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 10L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
    public static void onChunkLoad(ChunkLoadingEvent.Load event) {
        //Constants.LOG.info("Chunk loaded: " + event.getChunkPos());
        if( event.getLevel().isClientSide() ) return;
        POOL.submit(() -> threadAddChunkBlock(event));
        //Log the total tasks and total completed tasks:
        if( POOL.getTaskCount() % 16 == 0)
            LoggerBase.logInfo(null, "000000", "TASKS: " + POOL.getTaskCount() + " COMPLETED: " + POOL.getCompletedTaskCount());
    }

    public static void threadAddChunkBlock(ChunkLoadingEvent.Load event)
    {
        final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();
        ChunkAccess c = event.getChunk();
        BlockPos p = c.getPos().getWorldPosition();
        LevelAccessor level = event.getLevel();

        //Use MangedChunk.loadedChunks to determine when chunk is loaded
        while( !ManagedChunkUtilityAccessor.isLoaded(level, c) ) {}

        final int RANGE = 1;
        //final int Y = new Random(p.getX()*p.getZ()).nextInt(128);
        final int Y = 128;
        BlockPos pos = null;
        for(int x = 0; x < RANGE; x++) {
            for(int z = 0; z < RANGE; z++) {
                pos = new BlockPos(p.getX()+x, Y, p.getZ()+z);
                blocks.add(Pair.of(GOLD, new BlockPos(p.getX()+x, Y, p.getZ()+z )));
            }
        }
        //LoggerBase.logInfo(null, "000000", "1) ADD: " + pos);

        int startTime = (int) System.currentTimeMillis();
        boolean succeeded = false;
        succeeded = ManagedChunk.updateChunkBlockStates( (ServerLevel) level, blocks);
        //LoggerBase.logInfo(null, "000000", "2) REMOVE: " + pos + " TIME: " + (endTime - startTime) + "ms " + " THREADS RUNNING: " + POOL.getTaskCount());
        //LoggerBase.logInfo(null, "000001", "2) CHUNK: " + pos + " SUCCEEDED: " + succeeded);


    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}