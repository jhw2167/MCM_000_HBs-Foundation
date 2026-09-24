package com.holybuckets.foundation.platform.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;


public interface ChunkLoader {

    boolean forceChunkLoad(ServerLevel level, ChunkPos pos);
    boolean unforceChunkLoad(ServerLevel level, ChunkPos pos);

    /**
     * Undo any player facing state the loader muted while a background load was in flight.
     * Safe to call when nothing is suppressed. Called on level unload and server stop so a
     * shutdown mid load cannot leave players muted.
     */
    default void restoreListeners() {}

    boolean isRunning(ServerLevel level);
}
