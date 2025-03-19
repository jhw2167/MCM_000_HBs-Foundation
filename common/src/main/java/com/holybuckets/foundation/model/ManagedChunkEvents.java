package com.holybuckets.foundation.model;

import com.google.gson.JsonElement;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.foundation.util.MixinManager;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.holybuckets.foundation.model.ManagedChunk.GENERAL_CONFIG;
import static com.holybuckets.foundation.model.ManagedChunk.MANAGED_SUBCLASSES;
import static com.holybuckets.foundation.model.ManagedChunk.LOADED_CHUNKS;
import static com.holybuckets.foundation.model.ManagedChunk.INITIALIZED_CHUNKS;

public class ManagedChunkEvents {

    public static final String CLASS_ID = "0015";

    public static void init( EventRegistrar reg )
    {
        reg.registerOnServerStopped(ManagedChunkEvents::onServerStopped);

        reg.registerOnLevelLoad(ManagedChunkEvents::onWorldLoad);
        reg.registerOnLevelUnload(ManagedChunkEvents::onWorldUnload);

        reg.registerOnChunkLoad(ManagedChunkEvents::onChunkLoad);
        reg.registerOnChunkUnload(ManagedChunkEvents::onChunkUnload);

        EventRegistrar.TickType tickType = EventRegistrar.TickType.ON_1200_TICKS;
        reg.registerOnServerTick(tickType, ManagedChunkEvents::onServerTick1200);
    }

    private static void onServerStopped(final ServerStoppedEvent event) {
        LOADED_CHUNKS.clear();
        INITIALIZED_CHUNKS.clear();
    }

    private static void onWorldLoad( final LevelLoadingEvent.Load event )
    {
        LevelAccessor level = event.getLevel();

        if(LOADED_CHUNKS.get(level) == null) {
            LOADED_CHUNKS.put(level, new ConcurrentHashMap<>());
            INITIALIZED_CHUNKS.put(level,  new ConcurrentSet<>());
        }

        if(level.isClientSide()) return;

        DataStore ds = GeneralConfig.getInstance().getDataStore();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( Constants.MOD_ID, level);

        JsonElement chunksIds = levelData.get("initializedChunkIds");
        if( chunksIds == null )
        {
            String[] ids = new String[0];
            levelData.addProperty("initializedChunkIds", HBUtil.FileIO.arrayToJson(ids));
            chunksIds = levelData.get("initializedChunkIds");
        }

        Set<String> initChunks = INITIALIZED_CHUNKS.get(level);
        chunksIds.getAsJsonArray().forEach( chunkId -> {
            initChunks.add(chunkId.getAsString());
        });

        Consumer<DatastoreSaveEvent> save = (datastoreSaveEvent) -> ManagedChunk.save(datastoreSaveEvent, level);
        EventRegistrar.getInstance().registerOnDataSave( save, EventPriority.Highest);
    }

    private static void onWorldUnload( final LevelLoadingEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide()) return;
        ManagedChunk.save(DatastoreSaveEvent.create(), level);
    }

    private static void onChunkLoad( final ChunkLoadingEvent.Load event )
    {
        LevelAccessor level = event.getLevel();

        String chunkId = HBUtil.ChunkUtil.getId(event.getChunk());
        ManagedChunk loadedChunk = LOADED_CHUNKS.get(level).get(chunkId);

        if(loadedChunk == null)
        {
            //LoggerBase.logDebug(null, "003008.1", "Creating new managed Chunk: " + chunkId);
            loadedChunk = new ManagedChunk(level, event.getChunkPos());
        }

        loadedChunk.handleChunkLoaded(event);
    }

    private static void onChunkUnload( final ChunkLoadingEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        ChunkAccess chunk = event.getChunk();
        String id = HBUtil.ChunkUtil.getId(chunk);
        ManagedChunk c = LOADED_CHUNKS.get(level).remove(id);

        if( c == null ) return;
        c.handleChunkUnloaded(event);
    }

    public static void onWorldTickStart(Level level) {
        ManagedChunkBlockUpdates.onWorldTick(level);
    }

    public static void onServerTick1200(ServerTickEvent event) {
        try {
            if(MixinManager.isEnabled("ManagedChunkEvents::onServerTick1200")) {
                ManagedChunkUtility.INSTANCES.values()
                    .stream().filter( m -> !m.level.isClientSide())
                    .forEach( m -> m.cullLevelChunks(event) );
            }
        } catch (Exception e) {
            MixinManager.recordError("ManagedChunkEvents::onServerTick1200", e);
        }

    }


}
//END CLASS