package com.holybuckets.foundation.platform.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;


public interface ChunkLoader {


    boolean forceChunkLoad(ServerLevel level, ChunkPos pos);
    boolean unforceChunkLoad(ServerLevel level, ChunkPos pos);

    default void restoreListeners() {}

    boolean isRunning(ServerLevel level);

    boolean isRunningOnLevel(ServerLevel level);
}
