package com.holybuckets.foundation.modelInterface;

import net.minecraft.world.level.LevelAccessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces a method to return ConcurrentHashMap of ManagedChunkData objects
 */
public interface IMangedChunkManager {

    public ConcurrentHashMap<String, IMangedChunkData> getManagedChunks();

    public Map<String, IMangedChunkManager> getManager(LevelAccessor level);
}
