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
import java.util.stream.Collectors;


/**
 * Description: Used to declutter the ManagedChunk class from static utility methods
 */
public class ManagedChunkUtilityAccessor {


    //** CHUNK STATUS **//
    public static boolean isLoaded(LevelAccessor level, String id)
    {
        if(level == null || id == null) return false;

        if(LOADED_CHUNKS.get(level) == null) return false;

        ManagedChunk mc = LOADED_CHUNKS.get(level).get(id);
        if(mc == null) return false;

        return true;
    }

    public static boolean isLoaded(LevelAccessor level, BlockPos p) {
        Level levelType = (Level) level;
        return levelType.isLoaded(p) && isLoaded(level, levelType.getChunk(p) );
    }

    public static boolean isLoaded(LevelAccessor level, ChunkAccess c) {
        if( c == null ) return false;
        return isLoaded(level , c.getPos());
    }

    public static boolean isLoaded(LevelAccessor level, ChunkPos p) {
        return isLoaded(level, HBUtil.ChunkUtil.getId(p));
    }


    /**
     * Provided a chunk and id, determines if the chunk and all chunks around it are loaded and
     * in FULL status, else returns false.
     * @param level
     * @param cp
     * @return
     */
    public static boolean isChunkFullyLoaded(LevelAccessor level, ChunkPos cp)
    {
        if(!isLoaded(level, cp)) return false;
        List<String> localAreaChunks = HBUtil.ChunkUtil.getLocalChunkIds(cp, 1);
        return localAreaChunks.stream().allMatch( chunkId -> isLoaded(level, chunkId) );
    }

    public static boolean isChunkFullyLoaded(LevelAccessor level, String id) {
        return isChunkFullyLoaded(level, HBUtil.ChunkUtil.getChunkPos(id));
    }

    public static boolean isChunkFullyLoaded(LevelAccessor level, BlockPos pos) {
        return isChunkFullyLoaded(level, HBUtil.ChunkUtil.getChunkPos(pos) );
    }


    //** UTILITIES **//

    /**
     * Get a copy of the set of all initialized chunks for this world
     * @param level
     * @return A copy of the set the all initialized chunks for this world
     */
    @Nullable
    public static Set<String> getInitializedChunks(LevelAccessor level)
    {
        if(level == null) return null;
        if(INITIALIZED_CHUNKS.get(level) == null) return null;
        return INITIALIZED_CHUNKS.get(level).stream().collect(Collectors.toSet());
    }

    /**
     * Get a chunk from a level using a chunk id
     * @param level
     * @param chunkId
     * @param forceLoad is deprecated, it causes too man issues
     * @return
     */
    public static LevelChunk getChunk(LevelAccessor level, String chunkId, boolean forceLoad)
    {
        if(level == null || chunkId == null) return null;
        if(LOADED_CHUNKS.get(level) == null) return null;
        if( !LOADED_CHUNKS.get(level).containsKey(chunkId) ) return null;

        return LOADED_CHUNKS.get(level).get(chunkId).getLevelChunk();
    }

    /*
    public static void forceUnloadChunk(LevelAccessor level, String chunkId)
    {
        ManagedChunk c = getManagedChunk(level, chunkId);
        LevelChunk chunk = c.getChunk(false);

        if( chunk == null )
            return;

        if( level instanceof ServerLevel )
            ((ServerLevel) level).unload( chunk );
    }
    */

    public static ManagedChunk getManagedChunk(LevelAccessor level, String id)
    {
        if(level == null || id == null) return null;
        if(LOADED_CHUNKS.get(level) == null) return null;

        return LOADED_CHUNKS.get(level).get(id);
    }


    /**
     * Get a new instance of random object unique to this chunks coordinates
     * and the Minecraft world seed value
     * @return
     */
    public static Random getChunkRandom(ChunkPos pos)
    {
        final GeneralConfig CONFIG = GeneralConfig.getInstance();
        return getChunkRandom(pos, CONFIG.getWorldSeed());
    }
    public static Random getChunkRandom(ChunkPos pos, final Long SEED)
    {
        Double RAND = HBUtil.ChunkUtil.getChunkPos1DMap(pos) *1d;
        if( RAND.equals( 0d ) )
            return new Random( SEED );

        RAND = SEED / RAND;
        if( RAND < 1d && RAND > -1d)
            RAND = 1d / RAND;

        return new Random( RAND.intValue() );
    }

    //##        END UTILITIES      ##//

    //## ######################## ##//




}
//END CLASS