package com.holybuckets.foundation.model;

import com.holybuckets.foundation.datastructure.ConcurrentCircularList;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import java.lang.ref.WeakReference;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ManagedChunkBlockUpdates {

    private static final String CLASS_ID = "013";
    private static final int MAX_ATTEMPTS = 5;

     private LevelAccessor level;


     private List<Pair<BlockState, BlockPos>> UPDATES;
     private Iterator<Pair<BlockState, BlockPos>> NEXT_UPDATE;
     private Map<Integer, Integer> PENDING;
     private Map<Integer, WeakReference<Pair<BlockState, BlockPos>>> SUCCEEDED;

     private WriteMonitor writeMonitor = new WriteMonitor(100);

    static final Map<LevelAccessor, ManagedChunkBlockUpdates> LEVEL_UPDATES = new ConcurrentHashMap<>();

     //Constructors
        public ManagedChunkBlockUpdates(LevelAccessor level)
        {
            this.level = level;
            this.PENDING = new ConcurrentHashMap<>();
            this.UPDATES = new ConcurrentCircularList<>();
            this.SUCCEEDED = new ConcurrentHashMap<>();
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

    private void updateBlockStates()
    {
        if( UPDATES.isEmpty() ) return;

        if( !writeMonitor.canWrite() ) {
            //if( monitor.canWriteNextTick() )
                //monitor.lock.lock();
            return;
        }

        if(this.NEXT_UPDATE == null)
            this.NEXT_UPDATE = UPDATES.iterator();

        for(int i = 0; i < writeMonitor.writesPerTick; i++)
        {

            if( !NEXT_UPDATE.hasNext() ) {
                NEXT_UPDATE = UPDATES.iterator();
                break;
            }

            Pair<BlockState, BlockPos> update = NEXT_UPDATE.next();
            if( update == null ) continue;

            if( updateBlockState(update) )
            {
                PENDING.remove(update.hashCode());
                SUCCEEDED.put(update.hashCode(), new WeakReference<>(update));
            }
            else if( PENDING.get(update.hashCode()) < MAX_ATTEMPTS )
            {
                PENDING.put(update.hashCode(), PENDING.get(update.hashCode()) + 1);
            }
            else {
                PENDING.remove(update.hashCode());
            }
        }

        //monitor.lock.unlock();
    }

    private boolean updateBlockState(Pair<BlockState, BlockPos> update) {
        BlockState state = update.getLeft();
        BlockPos pos = update.getRight();
        int tag = Block.UPDATE_IMMEDIATE;

        return level.setBlock(pos, state, tag);
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

    static boolean updateChunkBlockStates(LevelAccessor level, List<Pair<BlockState, BlockPos>> updates)
    {
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

    static void init(EventRegistrar registrar) {
        registrar.registerOnLevelLoad(ManagedChunkBlockUpdates::onLoadWorld);
        registrar.registerOnLevelUnload(ManagedChunkBlockUpdates::onUnloadWorld);
    }

    private static void onLoadWorld(LevelLoadingEvent.Load event) {
        LEVEL_UPDATES.put(event.getLevel(), new ManagedChunkBlockUpdates(event.getLevel()));
    }

    private static void onUnloadWorld(LevelLoadingEvent.Unload event) {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.remove(event.getLevel());
        manager.clear();
    }


    static void onWorldTick(Level level) {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return;
        manager.updateBlockStates();
    }



    private class WriteMonitor {

        private static long TICKS_PER_SECOND = 20;
        private long writesPerSecond;
        long writesPerTick;
        private final long MODULO_COUNT = 4;
        private long tickCount;

        final ReentrantLock lock = new ReentrantLock();

        public WriteMonitor(long writesPerSecond)
        {
            this.writesPerSecond = writesPerSecond;

            if( writesPerSecond <= 0 )
                this.writesPerSecond = 10;

            this.writesPerTick = writesPerSecond / (TICKS_PER_SECOND / MODULO_COUNT);

            this.tickCount = 0;
        }


        public boolean canWrite()
        {
            if( tickCount % MODULO_COUNT == 0 )
            {
                tickCount++;
                return true;
            }
            tickCount++;
            return false;
        }

        public boolean canWriteNextTick() {
            return tickCount % MODULO_COUNT == 0;
        }

    }

}
