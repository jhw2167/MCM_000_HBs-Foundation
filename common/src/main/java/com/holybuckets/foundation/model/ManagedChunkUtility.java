package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;

import static com.holybuckets.foundation.model.ManagedChunk.LOADED_CHUNKS;
import static com.holybuckets.foundation.model.ManagedChunk.INITIALIZED_CHUNKS;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Description: Used to declutter the ManagedChunk class from static utility methods
 */
public class ManagedChunkUtility {
    static final Map<Level, ManagedChunkUtility> INSTANCES = new ConcurrentHashMap<>();
    final LevelAccessor level;
    private Thread cullLevelChunksThread;

    private ManagedChunkUtility(LevelAccessor level) {
        this.level = level;
    }

    public static ManagedChunkUtility getInstance(LevelAccessor level) {
        if (!(level instanceof Level)) return null;
        return INSTANCES.computeIfAbsent((Level)level, k -> new ManagedChunkUtility(level));
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnLevelLoad(ManagedChunkUtility::onWorldLoad, EventPriority.High);
        reg.registerOnLevelUnload(ManagedChunkUtility::onWorldUnload, EventPriority.Low);
    }

    //** CHUNK STATUS **//
    public boolean isLoaded(String id) {
        if(id == null) return false;

        if(LOADED_CHUNKS.get(level) == null) return false;

        ManagedChunk mc = LOADED_CHUNKS.get(level).get(id);
        if(mc == null) return false;

        return true;
    }

    public boolean isLoaded(BlockPos p) {
        Level levelType = (Level) level;
        return levelType.isLoaded(p) && isLoaded(levelType.getChunk(p));
    }

    public boolean isLoaded(ChunkAccess c) {
        if(c == null) return false;
        return isLoaded(c.getPos());
    }

    public boolean isLoaded(ChunkPos p) {
        return isLoaded(HBUtil.ChunkUtil.getId(p));
    }


    /**
     * Provided a chunk and id, determines if the chunk and all chunks around it are loaded and
     * in FULL status, else returns false.
     * @param cp ChunkPos to check
     * @return boolean indicating if chunk and surroundings are fully loaded
     */
    public boolean isChunkFullyLoaded(ChunkPos cp) {
        if(!isLoaded(cp)) return false;
        List<String> localAreaChunks = HBUtil.ChunkUtil.getLocalChunkIds(cp, 1);
        return localAreaChunks.stream().allMatch(this::isLoaded);
    }

    public boolean isChunkFullyLoaded(String id) {
        return isChunkFullyLoaded(HBUtil.ChunkUtil.getChunkPos(id));
    }

    public boolean isChunkFullyLoaded(BlockPos pos) {
        return isChunkFullyLoaded(HBUtil.ChunkUtil.getChunkPos(pos));
    }


    //** STATICS **//

    public static boolean isChunkFullyLoaded(LevelAccessor level, String id) {
        ManagedChunkUtility util = getInstance(level);
        return util.isChunkFullyLoaded(id);
    }



    //** UTILITIES **//

    /**
     * Get a copy of the set of all initialized chunks for this world
     * @return A copy of the set of all initialized chunks for this world
     */
    @Nullable
    public Set<String> getInitializedChunks() {
        if(INITIALIZED_CHUNKS.get(level) == null) return null;
        return INITIALIZED_CHUNKS.get(level).stream().collect(Collectors.toSet());
    }

    /**
     * Get a chunk using a chunk id
     * @param chunkId The chunk ID
     * @param forceLoad is deprecated, it causes too many issues
     * @return LevelChunk or null
     */
    public LevelChunk getChunk(String chunkId, boolean forceLoad) {
        if(chunkId == null) return null;
        if(LOADED_CHUNKS.get(level) == null) return null;
        if(!LOADED_CHUNKS.get(level).containsKey(chunkId)) return null;

        return LOADED_CHUNKS.get(level).get(chunkId).getLevelChunk();
    }

    public ManagedChunk getManagedChunk(String id) {
        if(id == null) return null;
        if(LOADED_CHUNKS.get(level) == null) return null;

        return LOADED_CHUNKS.get(level).get(id);
    }

    //* STATICS

    public static ManagedChunk getManagedChunk(LevelAccessor level, String id) {
        if(id == null || level == null) return null;
        if(LOADED_CHUNKS.get(level) == null) return null;

        return LOADED_CHUNKS.get(level).get(id);
    }

    /**
     * Get a new instance of random object unique to this chunks coordinates
     * and the Minecraft world seed value
     * @return Random instance
     */
    public static Random getChunkRandom(ChunkPos pos) {
        final GeneralConfig CONFIG = GeneralConfig.getInstance();
        return getChunkRandom(pos, CONFIG.getWorldSeed());
    }

    public static Random getChunkRandom(ChunkPos pos, final Long SEED) {
        Double RAND = HBUtil.ChunkUtil.getChunkPos1DMap(pos) * 1d;
        if(RAND.equals(0d))
            return new Random(SEED);

        RAND = SEED / RAND;
        if(RAND < 1d && RAND > -1d)
            RAND = 1d / RAND;

        return new Random(RAND.intValue());
    }


    //*
    void cullLevelChunks(ServerTickEvent event) {
        if (cullLevelChunksThread != null) {
            cullLevelChunksThread.interrupt();
        }
        cullLevelChunksThread = new Thread(this::cullLevelChunks);
        cullLevelChunksThread.start();
    }

    /**
     * Checks to make sure we are not holding a reference to a levelChunk that is trying to unload
     *
     */
    private void cullLevelChunks()
    {
        if(LOADED_CHUNKS.get(level) == null) return;

        Map<String, ManagedChunk> loadedChunks = LOADED_CHUNKS.get(level);
        boolean cull = loadedChunks.size() > 16_000;
        for(ManagedChunk m : loadedChunks.values())
        {
            boolean isFullyLoaded = m.util.isChunkFullyLoaded(m.getChunkPos());
            boolean hasLevelChunk = m.getLevelChunk() != null;

            if( isFullyLoaded && hasLevelChunk) {
                int i =0; //nothing
            }
            else if( !isFullyLoaded  && !hasLevelChunk ) {
                int i =0; //nothing
            }
            else if( !isFullyLoaded  && hasLevelChunk && cull ) {
                m.releaseLevelChunk();
            }
            else if( !isFullyLoaded  && hasLevelChunk && !cull )
            {
                List<String> localChunks = HBUtil.ChunkUtil.getLocalChunkIds(m.getChunkPos(), 1);
                long countLoaded = localChunks.stream().filter( s -> m.util.isLoaded(s)).count();
                if( countLoaded == 0 ) m.releaseLevelChunk();
            }
            else if( isFullyLoaded  && !hasLevelChunk ) {
                ChunkPos cp = m.getChunkPos();
                m.levelChunk = HBUtil.ChunkUtil.getLevelChunk(level, cp.x, cp.z, false);
            }

        }
    }


    //* Boilerplate

    private void shutdown () 
    {
        if(this.cullLevelChunksThread != null) {
                this.cullLevelChunksThread.interrupt();
                this.cullLevelChunksThread = null;
        }
    }


    //* Events

    public static void onWorldLoad(LevelLoadingEvent.Load event) {
        getInstance(event.getLevel());
    }

    public static void onWorldUnload(LevelLoadingEvent.Unload event) {
        ManagedChunkUtility util = INSTANCES.remove(event.getLevel());
        if(util != null) util.shutdown();
    }

}
