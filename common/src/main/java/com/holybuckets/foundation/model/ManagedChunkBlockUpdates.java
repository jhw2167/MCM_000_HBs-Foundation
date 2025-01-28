package com.holybuckets.foundation.model;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentCircularList;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import java.lang.ref.WeakReference;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ManagedChunkBlockUpdates {

    private static final String CLASS_ID = "013";
    private static final int MAX_ATTEMPTS = 5;

     private LevelAccessor level;


     private List<Pair<BlockState, BlockPos>> UPDATES;
     private Map<Integer, Integer> PENDING;
    private Map<Integer, WeakReference<Pair<BlockState, BlockPos>>> SUCCEEDED;

     private WriteMonitor monitor = new WriteMonitor(10);

    static final Map<LevelAccessor, ManagedChunkBlockUpdates> LEVEL_UPDATES = new ConcurrentHashMap<>();

     //Constructors
        public ManagedChunkBlockUpdates(LevelAccessor level)
        {
            this.level = level;
            this.PENDING = new ConcurrentHashMap<>();
            this.UPDATES = new ConcurrentCircularList<>();
        }


    private void addUpdate(Pair<BlockState, BlockPos> update) {

        if( update == null || PENDING.containsKey(update.hashCode()) )
            return;

        PENDING.put(update.hashCode(), 0);
        UPDATES.add(update);
    }

    /**
     * Check if the update has succeeded and removes it.
     * @param hashCode
     * @return true if the update has succeeded
     */
    boolean checkSucceeded(int hashCode) {
        return SUCCEEDED.remove(hashCode) != null;
    }

    private void clear() {
        PENDING.clear();
        UPDATES.clear();
    }


    //** UPDATING CHUNK BLOCKS **//

    static boolean updateChunkBlocks(LevelAccessor level, Map<BlockState, List<BlockPos>> updates)
    {
        List<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();
        for(Map.Entry<BlockState, List<BlockPos>> update : updates.entrySet()) {
            for(BlockPos pos : update.getValue()) {
                blockStates.add( Pair.of(update.getKey(), pos) );
            }
        }
        return updateChunkBlockStates(level, blockStates);
    }


    static boolean updateChunkBlocks(LevelAccessor level, List<Pair<Block, BlockPos>> updates)
    {
        List<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();
        for(Pair<Block, BlockPos> update : updates) {
            blockStates.add( Pair.of(update.getLeft().defaultBlockState(), update.getRight()) );
        }

        return updateChunkBlockStates(level, blockStates);
    }

    static boolean updateChunkBlockStates(LevelAccessor level, List<Pair<BlockState, BlockPos>> updates) {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return false;
        updates.forEach(manager::addUpdate);

        return true;
    }

    static boolean checkUpdateBlockStateSucceeded(LevelAccessor level, Pair<BlockState, BlockPos> update) {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return false;
        return manager.checkSucceeded(update.hashCode());
    }

    //** EVENTS **//

    public static void init(EventRegistrar registrar) {
        registrar.registerOnLevelLoad(ManagedChunkBlockUpdates::onLoadWorld);
        registrar.registerOnLevelUnload(ManagedChunkBlockUpdates::onUnloadWorld);
    }

    public static void onLoadWorld(LevelLoadingEvent.Load event) {
        LEVEL_UPDATES.put(event.getLevel(), new ManagedChunkBlockUpdates(event.getLevel()));
    }

    public static void onUnloadWorld(LevelLoadingEvent.Unload event) {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.remove(event.getLevel());
        manager.clear();
    }


    private class WriteMonitor {

        private static long TICKS_PER_SECOND = 20;
        private long writesPerSecond;
        long writesPerTick;
        private final long moduloCount;
        private long tickCount;

        public WriteMonitor(long writesPerSecond)
        {
            this.writesPerSecond = writesPerSecond;

            if( writesPerSecond <= 0 )
                this.writesPerSecond = 10;

            if( writesPerSecond > TICKS_PER_SECOND ) {
                this.moduloCount = 1;
                this.writesPerTick = this.writesPerSecond / TICKS_PER_SECOND;
            } else {
                this.moduloCount = TICKS_PER_SECOND / this.writesPerSecond;
                this.writesPerTick = 1;
            }

            this.tickCount = 0;
        }


        public boolean canWrite()
        {
            if( tickCount % moduloCount == 0 )
            {
                tickCount++;
                return true;
            }
            tickCount++;
            return false;
        }

    }

}
