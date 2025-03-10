package com.holybuckets.foundation;

import com.google.common.eventbus.Subscribe;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkBlockUpdates;
import com.holybuckets.foundation.model.ManagedChunkUtilityAccessor;
import com.holybuckets.foundation.platform.Services;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Consumer;


public class CommonClass {

    public static boolean isInitialized = false;
    public static void init()
    {

        Constants.LOG.info("Loaded {} mod on {}! we are currently in a {} environment!", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        FoundationInitializers.init();
        //test(EventRegistrar.getInstance()); BalmEventRegister.registerEvents();

        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void test(EventRegistrar reg)
    {
        reg.registerOnChunkLoad(CommonClass::onChunkLoad);
        reg.registerOnLevelLoad(CommonClass::onLevelLoad);
        //reg.registerOnPlayerLoad(CommonClass::onPlayerLoad);
        reg.registerOnServerTick(EventRegistrar.TickType.ON_1200_TICKS , CommonClass::onServerTick);
    }

    static void onServerTick(ServerTickEvent event) {
        Constants.LOG.info("Server ticked: " + event.getTickCount() );
    }

    private static void onPlayerLoad(PlayerLoginEvent event) {
        Constants.LOG.info("Player loaded: " + event.getPlayer().getGameProfile().getName());
    }


    //Create a threadpool to add blocks to a chunk, max 16 threads, store in queue
    public static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 10L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
    public static void onChunkLoad(ChunkLoadingEvent.Load event)
    {
        if( event.getLevel().isClientSide() ) return;

        String id = HBUtil.ChunkUtil.getId(event.getChunk().getPos().getWorldPosition());
        String chunkId = HBUtil.ChunkUtil.getId(event.getChunk());

        //Print ids to test if they match
        Constants.LOG.info("Chunk loaded: " + id + " " + chunkId + " MATCH: " + id.equals(chunkId) );

        POOL.submit(() -> threadAddChunkBlock(event));
    }

    public static void threadAddChunkBlock(ChunkLoadingEvent.Load event)
    {
        //final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        final BlockState GOLDSTATE = ModBlocks.empty.defaultBlockState();
        final Block GOLD = ModBlocks.empty;
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();
        ChunkAccess c = event.getChunk();
        BlockPos p = c.getPos().getWorldPosition();
        LevelAccessor level = event.getLevel();

        //Use MangedChunk.loadedChunks to determine when chunk is loaded
        while( !ManagedChunkUtilityAccessor.isLoaded(level, c) ) {}

        //Add blocks to chunk
        List<Pair<BlockState, BlockPos>> blockStateList = new ArrayList<>();
        blockStateList.add(Pair.of(GOLDSTATE, p));
        ManagedChunk.updateChunkBlockStates(level, blockStateList);

        List<Pair<Block, BlockPos>> blockList = new ArrayList<>();
        blockList.add(Pair.of(GOLD, p));
        ManagedChunk.updateChunkBlocks(level, blockList);

    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}