package com.holybuckets.foundation.core;

import com.holybuckets.foundation.platform.services.ChunkLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Stub. Chunk Pregenerator is forge only, so there is no distant chunk loading
 * strategy on fabric yet; the explorer stays idle.
 */
public class FabricChunkLoader implements ChunkLoader {

    @Override
    public boolean forceChunkLoad(ServerLevel level, ChunkPos pos) {
        return false;
    }

    @Override
    public boolean unforceChunkLoad(ServerLevel level, ChunkPos pos) {
        return false;
    }
}
