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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ManagedChunkBlockUpdates {

    private static final String CLASS_ID = "013";

     private LevelAccessor level;
     private Set<Integer> PENDING;
     private List<Pair<BlockState, BlockPos>> UPDATES;

     private WriteMonitor monitor = new WriteMonitor(10);

    static final Map<LevelAccessor, ManagedChunkBlockUpdates> LEVEL_UPDATES = new ConcurrentHashMap<>();

     //Constructors
        public ManagedChunkBlockUpdates(LevelAccessor level)
        {
            this.level = level;
            this.PENDING = new HashSet<>();
            this.UPDATES = new ConcurrentCircularList<>();
        }


    //** UPDATING CHUNK BLOCKS **//

    public static boolean updateChunkBlocks(LevelAccessor level, Map<BlockState, List<BlockPos>> updates)
    {
        List<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();
        for(Map.Entry<BlockState, List<BlockPos>> update : updates.entrySet()) {
            for(BlockPos pos : update.getValue()) {
                blockStates.add( Pair.of(update.getKey(), pos) );
            }
        }
        return updateChunkBlockStatesAndCast(level, blockStates);
    }


    public static boolean updateChunkBlocks(LevelAccessor level, List<Pair<Block, BlockPos>> updates)
    {
        List<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();
        for(Pair<Block, BlockPos> update : updates) {
            blockStates.add( Pair.of(update.getLeft().defaultBlockState(), update.getRight()) );
        }

        return updateChunkBlockStatesAndCast(level, blockStates);
    }

    static boolean updateChunkBlockStatesAndCast(LevelAccessor level, List<Pair<BlockState, BlockPos>> updates)
    {
        if( level instanceof ServerLevel)
            return updateChunkBlockStates((ServerLevel) level, updates);
        else if( level instanceof ClientLevel)
            return updateChunkBlockStates((ClientLevel) level, updates);
        else
            return false;
    }

    /**
     * Update the block states of a chunk on a ServerLevel. It is important that it is synchronized to prevent
     * concurrent modifications to the chunk. The block at the given position is updated to the requisite
     * block state.
     * @param serverLevel
     * @param updates
     * @return true if successful, false if some element was null
     */
    public static boolean updateChunkBlockStates(final ServerLevel serverLevel, List<Pair<BlockState, BlockPos>> updates)
    {
        if( serverLevel == null || updates == null || updates.size() == 0 )
            return false;
        //LoggerBase.logDebug(null, "003022", "Updating chunk block states: " + HolyBucketsUtility.ChunkUtil.getId( chunk));

        boolean succeeded = false;
        ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(serverLevel);
        try
        {
            for(Pair<BlockState, BlockPos> update : updates)
            {
                if( !serverLevel.isLoaded( update.getRight() ) ) {
                    succeeded = false;
                    continue;
                }
                manager.addUpdate(update);
            }

            Map<BlockState, List<BlockPos>> blockStates = HBUtil.BlockUtil.condenseBlockStates(updates);
            BlockStateUpdatesMessage.createAndFire(serverLevel, blockStates);
        }
        catch (IllegalStateException e)
        {
            LoggerBase.logWarning(null, "003023", "Illegal state exception " +
                "updating chunk block states. Updates may be replayed later. At level: " + HBUtil.LevelUtil.toLevelId(serverLevel) );
            return false;
        }
        catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error updating chunk block states. At level: ");
            error.append( HBUtil.LevelUtil.toLevelId(serverLevel) );
            error.append("\n");
            error.append("Corresponding exception message: \n");
            error.append(e.getMessage());

            LoggerBase.logError(null, "003024", error.toString());

            return false;
        }

        return true;
    }

    private void addUpdate(Pair<BlockState, BlockPos> update) {

        if( update == null || PENDING.contains(update.hashCode()) )
            return;

        PENDING.add(update.hashCode());
        UPDATES.add(update);
    }

    private void clear() {
        PENDING.clear();
        UPDATES.clear();
    }

    /**
     * Update the block states of a chunk on a ClientLevel. It is important that it is synchronized to prevent
     * concurrent modifications to the chunk. The block at the given position is updated to the requisite
     * block state.
     * @param clientLevel
     * @param updates
     * @return true if successful, false if some element was null
     */
    public static boolean updateChunkBlockStates(final ClientLevel clientLevel, List<Pair<BlockState, BlockPos>> updates)
    {
        if( clientLevel == null || updates == null || updates.size() == 0 )
            return false;
        //LoggerBase.logDebug(null, "003022", "Updating chunk block states: " + HolyBucketsUtility.ChunkUtil.getId( chunk));

        try
        {
            ManagedChunkBlockUpdates manager = LEVEL_UPDATES.get(clientLevel);
            boolean isSuccessful = true;
            for(Pair<BlockState, BlockPos> update : updates)
            {
                if( !clientLevel.isLoaded( update.getRight() ) ) {
                    isSuccessful = false;
                    continue;
                }
                manager.addUpdate(update);
            }

            return isSuccessful;
        }
        catch (IllegalStateException e)
        {
            LoggerBase.logWarning(null, "003023", "Illegal state exception " +
                "updating chunk block states. Updates may be replayed later. At level: " + HBUtil.LevelUtil.toLevelId(clientLevel) );
            return false;
        }
        catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error updating chunk block states. At level: ");
            error.append( HBUtil.LevelUtil.toLevelId(clientLevel) );
            error.append("\n");
            error.append("Corresponding exception message: \n");
            error.append(e.getMessage());

            LoggerBase.logError(null, "003024", error.toString());

            return false;
        }
    }

    //## END UPDATING CHUNK BLOCKS ##//

    //## ######################## ##//


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
