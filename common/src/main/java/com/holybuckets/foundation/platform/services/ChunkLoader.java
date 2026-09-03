package com.holybuckets.foundation.platform.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;


public interface ChunkLoader {

    /**
     * Submites a chunk pos to be loaded
     * @param level
     * @param pos
     * @return
     */
    boolean forceChunkLoad(ServerLevel level, ChunkPos pos);

    /**
     *
     * @param level
     * @param pos
     * @return false if a process is still running, true if we can move on and load a new chunk
     */
    boolean unforceChunkLoad(ServerLevel level, ChunkPos pos);
}
