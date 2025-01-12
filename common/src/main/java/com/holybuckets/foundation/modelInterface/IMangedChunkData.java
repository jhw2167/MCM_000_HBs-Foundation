package com.holybuckets.foundation.modelInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;

public interface IMangedChunkData  {

    /**
     * Initialize the ManagedChunk and underlying data from memory or
     * perform any necessary configuration
     */
    boolean isInit(String subclass);

    /**
     * Create a dummy constructor to call this method and return a reference to an
     * existing instance of the class sitting in RAM. I am aware this is a hack and
     * not an elegant implementation
     * @return IMangedChunkData
     */
    public IMangedChunkData getStaticInstance(LevelAccessor level, String id);

    /**
     * @param event
     */
    //public void handleChunkLoaded(ChunkEvent.Load event);

    /**
     * Fired when a chunk is unloaded from memory
     * @param chunk
     */
    //public void handleChunkUnloaded(ChunkEvent.Unload event);


    public CompoundTag serializeNBT();

    public void deserializeNBT(CompoundTag nbt);

}
