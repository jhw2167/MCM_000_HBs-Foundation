package com.holybuckets.foundation.core;

import com.holybuckets.foundation.platform.services.ChunkLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class FabricChunkLoader implements ChunkLoader {

    @Override
    public boolean forceChunkLoad(ServerLevel level, ChunkPos pos) {
        return false;
    }

    @Override
    public boolean unforceChunkLoad(ServerLevel level, ChunkPos pos) {
        return false;
    }

    @Override
    public boolean isRunning(ServerLevel level) {
        return false;
    }

    @Override
    public boolean isRunningOnLevel(ServerLevel level) {
        return false;
    }
}
