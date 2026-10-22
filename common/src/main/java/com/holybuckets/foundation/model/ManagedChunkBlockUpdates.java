package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ManagedChunkBlockUpdates {

    private static final String CLASS_ID = "013";
    private static final int MAX_ATTEMPTS = 5;
    private final ManagedChunkUtility util;

    private LevelAccessor level;

     private Queue<Pair<BlockState, BlockPos>> UPDATES;

     private WriteMonitor writeMonitor;

    static final Map<LevelAccessor, ManagedChunkBlockUpdates> LEVEL_UPDATES = new ConcurrentHashMap<>();

     //Constructors
        public ManagedChunkBlockUpdates(LevelAccessor level)
        {
            this.level = level;
            this.util = ManagedChunkUtility.getInstance(level);
            this.UPDATES = new ConcurrentLinkedQueue<>();
            writeMonitor = new WriteMonitor();

        }

    private void addUpdate(Pair<BlockState, BlockPos> update) {
        UPDATES.add(update);
    }

    private void clear() {
        UPDATES.clear();
    }

    private void updateBlockStates()
    {
        try {
            int writesPerTick = writeMonitor.getWritesPerTick();
            Iterator<?> next = UPDATES.iterator();
            for (int i = 0; i < writesPerTick && next.hasNext(); i++) {
                Pair<BlockState, BlockPos> update = (Pair<BlockState, BlockPos>) next.next();
                if( update == null ) continue;
                if( !this.util.isLoaded(update.getRight()) ) continue;
                level.setBlock(update.getRight(), update.getLeft(), Block.UPDATE_IMMEDIATE);
                next.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shutdown() {
        if( writeMonitor != null ) {
            //writeMonitor.shutdown();
        }
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
        if( updates == null || updates.isEmpty() )
            return false;
        if( level.isClientSide() ) return false;
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return false;

        //Filter out any blockStates that have hbs_foundaton:empty_block default state
        List<Pair<BlockState, BlockPos>> mutableUpdates = new ArrayList<>(updates);
        mutableUpdates.removeIf(p -> p.getLeft().equals(ModBlocks.empty.get().defaultBlockState()));

        for(Pair<BlockState, BlockPos> update : mutableUpdates) {
            if(update==null && manager.util.isLoaded( update.getRight() )) continue;
            level.setBlock( update.getRight(), update.getLeft(), Block.UPDATE_ALL_IMMEDIATE | Block.UPDATE_CLIENTS );
        }

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
        if( manager == null ) return;
        manager.shutdown();
        manager.clear();
    }


    static void onWorldTick(Level level) {
        if(level.isClientSide) return;
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(level);
        if( manager == null ) return;
        //manager.updateBlockStates();
    }



    private class WriteMonitor {

        private static int TICKS_PER_SECOND = 20;
        private AtomicInteger writesPerTick;
        private final int MODULO_COUNT = 4;
        private volatile int tickCount;

        private volatile Thread currentWorker;
        private Thread currentMain;

        final ReentrantLock lock = new ReentrantLock(false);

        public WriteMonitor()
        {
            GeneralConfig config = GeneralConfig.getInstance();
            this.writesPerTick = config.getPerformanceImpactConfig().getBlockWritesPerTick();
            this.tickCount = 0;
        }

        Integer getWritesPerTick() {
            return writesPerTick.get() * MODULO_COUNT;
        }

        boolean tryLockMainThread() {
            this.currentMain = Thread.currentThread();
            if( canWrite() ) {
                return lock.tryLock();
            }
            return false;
        }

        boolean tryLockWorkerThread() {
            this.currentWorker = Thread.currentThread();
            if( softLock() || lock.isLocked() ) {
            try {
                Thread.sleep(50);
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

        private void shutdown() {
        if(lock.isLocked()) lock.unlock();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if( currentWorker != null && currentWorker.isAlive() )
                currentWorker.interrupt();
        }

    }

}
