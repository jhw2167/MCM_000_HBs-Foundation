package com.holybuckets.foundation.model;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentCircularList;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessageHandler;
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

        try {

            if( !writeMonitor.tryLockMainThread() )
                return;

            if( UPDATES.isEmpty() ) return;

            if (this.NEXT_UPDATE == null || !this.NEXT_UPDATE.hasNext()) {
                this.NEXT_UPDATE = UPDATES.iterator();
                if (!this.NEXT_UPDATE.hasNext()) return; // No updates to process
            }

            for (int i = 0; i < writeMonitor.writesPerTick && NEXT_UPDATE.hasNext(); i++) {
                Pair<BlockState, BlockPos> update = NEXT_UPDATE.next();
                if (update == null) continue;

                if(!ableToUpdateBlock(update))
                    continue;

                if (updateBlockState(update)) {
                    NEXT_UPDATE.remove();
                    //PENDING.remove(update.hashCode());
                    SUCCEEDED.put(update.hashCode(), new WeakReference<>(update));
                } else if (PENDING.get(update.hashCode()) < MAX_ATTEMPTS) {
                    PENDING.put(update.hashCode(), PENDING.get(update.hashCode()) + 1);
                } else {
                    NEXT_UPDATE.remove();
                    //PENDING.remove(update.hashCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeMonitor.unlock();
        }

    }

    private boolean updateBlockState(Pair<BlockState, BlockPos> update) {
        BlockState state = update.getLeft();
        BlockPos pos = update.getRight();
        int tag = Block.UPDATE_IMMEDIATE;

        return level.setBlock(pos, state, tag);
    }

    private boolean ableToUpdateBlock(Pair<BlockState, BlockPos> update) {
        return ManagedChunkUtilityAccessor.isLoaded(level, update.getRight());
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

    static synchronized boolean updateChunkBlockStates(LevelAccessor level, List<Pair<BlockState, BlockPos>> updates)
    {
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return false;

        try {
            if( !manager.writeMonitor.tryLockWorkerThread() )
                return false;
            updates.forEach(manager::addUpdate);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            manager.writeMonitor.unlock();
        }

        if( !level.isClientSide() )
            return sendChunkBlockStatesToClient(level, updates);

        return true;
    }

    static boolean sendChunkBlockStatesToClient(LevelAccessor level, List<Pair<BlockState, BlockPos>> updates)
    {
        try {
            Map<BlockState, List<BlockPos>> blockStateData = HBUtil.BlockUtil.condenseBlockStates(updates);
            BlockStateUpdatesMessage.createAndFire(level, blockStateData);
        } catch (Exception e) {
            LoggerBase.logError(null,"013001", "Failed to send block state updates to client");
            return false;
        }
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
        private volatile long tickCount;

        final ReentrantLock lock = new ReentrantLock(false);

        public WriteMonitor(long writesPerSecond)
        {
            this.writesPerSecond = writesPerSecond;

            if( writesPerSecond <= 0 )
                this.writesPerSecond = 10;

            this.writesPerTick = writesPerSecond / (TICKS_PER_SECOND / MODULO_COUNT);

            this.tickCount = 0;
        }

        boolean tryLockMainThread() {
            if( canWrite() ) {
                return lock.tryLock();
            }
            return false;
        }

        boolean tryLockWorkerThread() {
            while( softLock() || lock.isLocked() )
            {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return lock.tryLock();
        }

        private boolean canWrite()
        {
            if( tickCount % MODULO_COUNT == 0 )
            {
                tickCount++;
                return true;
            }
            tickCount++;
            return false;
        }

        private boolean softLock() {
            return tickCount % MODULO_COUNT == 0;
        }

        public synchronized void unlock() {
            if( lock.isLocked() && lock.isHeldByCurrentThread() )
                lock.unlock();
        }
    }

}
