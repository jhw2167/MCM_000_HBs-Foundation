package com.holybuckets.foundation;

import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ClientInputEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.holybuckets.foundation.platform.Services;
import net.blay09.mods.balm.api.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Thread.sleep;


public class CommonClass {

    public static boolean isInitialized = false;
    public static void init()
    {

        Constants.LOG.info("Loaded {} mod on {}! we are currently in a {} environment!", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        FoundationInitializers.init();
        test(EventRegistrar.getInstance()); BalmEventRegister.registerEvents();

        isInitialized = true;
    }

    /**
     * Description: Run sample tests methods
     */
    public static void test(EventRegistrar reg)
    {
        //reg.registerOnChunkLoad(CommonClass::onChunkLoad);
        //reg.registerOnLevelLoad(CommonClass::onLevelLoad);
        //reg.registerOnPlayerLogin(CommonClass::onPlayerLogin);
        //reg.registerOnClientInput(CommonClass::onClientInput);
        reg.registerOnServerTick(TickType.ON_120_TICKS, CommonClass::on120Ticks);
        reg.registerOnServerTick(TickType.DAILY_TICK, CommonClass::onDailyTick);
        //reg.registerOnServerTick(TickType.ON_1200_TICKS , CommonClass::onServerTick);
    }

    private static void on120Ticks(ServerTickEvent event) {
        GeneralConfig config = GeneralConfig.getInstance();
        LoggerBase.logDebug(null, "001090", "Server ticks: " + config.getTotalTickCount());
        LoggerBase.logDebug(null, "001091", "Overworld ticks: " + config.getTotalTickCountWithSleep(GeneralConfig.OVERWORLD) );
        LoggerBase.logDebug(null, "001092", "Nether ticks: " + config.getTotalTickCountWithSleep(GeneralConfig.NETHER) );

    }

    private static void onDailyTick(ServerTickEvent.DailyTickEvent event) {
        GeneralConfig config = GeneralConfig.getInstance();
        LoggerBase.logDebug(null, "001100", "Daily tick: " + event.getTickCountWithSleeps());
        LoggerBase.logDebug(null, "001100", "Daily tick: " + event.isTriggeredByWakeUp() );
    }

    // Subscribe to client input events


    private static void onClientInput(ClientInputEvent event) {
        LoggerBase.logInfo(null, "001001", "Client Input Event - Keys pressed: " + event.getKeyCodes());

    }

    private static void onPlayerLogin(PlayerLoginEvent event) {
        Constants.LOG.info("Player connected CONNECTED: " + event.getPlayer().getGameProfile().getName());
        //Print out the location of each dimension by converting the resourceLocation to Level using HBUtil.LevelUtil
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

        if(!chunkId.equals("0,0"))
            return;
        //Print ids to test if they match
        Constants.LOG.info("Chunk loaded: " + id + " " + chunkId + " MATCH: " + id.equals(chunkId) );

        POOL.submit(() -> threadAddChunkBlock(event));
    }

    public static void threadAddChunkBlock(ChunkLoadingEvent.Load event)
    {
        //final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        final BlockState GOLDSTATE = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
        final Block GOLD = ModBlocks.empty;
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();
        ChunkAccess c = event.getChunk();
        BlockPos p = c.getPos().getWorldPosition();
        LevelAccessor level = event.getLevel();
        ManagedChunkUtility util = ManagedChunkUtility.getInstance(level);

        //Use MangedChunk.loadedChunks to determine when chunk is loaded
        while( !util.isChunkFullyLoaded(c.getPos()) ) {}

    try {
        sleep(5000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }


        //Add blocks to chunk
        //HBUtil.TripleInt[] sphere = HBUtil.ShapeUtil.getSphere(8, 32).toArray();
        List<Pair<BlockState, BlockPos>> blockStateList = new ArrayList<>();

        /*
        for(int i = 0; i < sphere.length; i++)
        {
            BlockPos pos = p.offset(sphere[i].x, sphere[i].y+120, sphere[i].z);
            blockStateList.add(Pair.of(GOLDSTATE, pos));
        }
        */

        LoggerBase.logInfo(null, "999000", "Added blocks to chunk: " + p);
        ManagedChunk.updateChunkBlockStates(level, blockStateList);


        //TestWorldPos
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(0, 15, 0);
        BlockPos p2a = new BlockPos(0, 16, 0);
        BlockPos p2b = new BlockPos(0, -15, 0);
        BlockPos p2c = new BlockPos(0, -16, 0);
        BlockPos p2d = new BlockPos(0, -17, 0);
        BlockPos p3 = new BlockPos(0, -64, 0);

        //Convert this blockPos to HBUtil.WorldPos and back, print the results
        HBUtil.WorldPos wp1 = new HBUtil.WorldPos(p1, c);
        HBUtil.WorldPos wp2 = new HBUtil.WorldPos(p2, c);
        HBUtil.WorldPos wp2a = new HBUtil.WorldPos(p2a, c);
        HBUtil.WorldPos wp2b = new HBUtil.WorldPos(p2b, c);
        HBUtil.WorldPos wp2c = new HBUtil.WorldPos(p2c, c);
        HBUtil.WorldPos wp2d = new HBUtil.WorldPos(p2d, c);
        HBUtil.WorldPos wp3 = new HBUtil.WorldPos(p3, c);

        Constants.LOG.info("WorldPos: " + wp1 + " " + wp1.worldPosToString() + " " + wp1.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2 + " " + wp2.worldPosToString() + " " + wp2.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2a + " " + wp2a.worldPosToString() + " " + wp2a.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2b + " " + wp2b.worldPosToString() + " " + wp2b.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2c + " " + wp2c.worldPosToString() + " " + wp2c.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2d + " " + wp2d.worldPosToString() + " " + wp2d.sectionToString());
        Constants.LOG.info("WorldPos: " + wp3 + " " + wp3.worldPosToString() + " " + wp3.sectionToString());





    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }

}
