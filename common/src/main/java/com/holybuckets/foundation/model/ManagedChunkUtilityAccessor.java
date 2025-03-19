package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
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
public class ManagedChunkUtilityAccessor {
    private static final Map<Level, ManagedChunkUtilityAccessor> INSTANCES = new ConcurrentHashMap<>();
    private final LevelAccessor level;

    private ManagedChunkUtilityAccessor(LevelAccessor level) {
        this.level = level;
    }

    public static ManagedChunkUtilityAccessor getInstance(LevelAccessor level) {
        if (!(level instanceof Level)) return null;
        return INSTANCES.computeIfAbsent((Level)level, k -> new ManagedChunkUtilityAccessor(level));
    }

    public static void onWorldLoad(Level level) {
        getInstance(level);
    }

    public static void onWorldUnload(Level level) {
        INSTANCES.remove(level);
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


    /**
     * Get a new instance of random object unique to this chunks coordinates
     * and the Minecraft world seed value
     * @return Random instance
     */
    public Random getChunkRandom(ChunkPos pos) {
        final GeneralConfig CONFIG = GeneralConfig.getInstance();
        return getChunkRandom(pos, CONFIG.getWorldSeed());
    }

    public Random getChunkRandom(ChunkPos pos, final Long SEED) {
        Double RAND = HBUtil.ChunkUtil.getChunkPos1DMap(pos) * 1d;
        if(RAND.equals(0d))
            return new Random(SEED);

        RAND = SEED / RAND;
        if(RAND < 1d && RAND > -1d)
            RAND = 1d / RAND;

        return new Random(RAND.intValue());
    }
}
