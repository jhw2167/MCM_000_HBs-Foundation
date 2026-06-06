package com.holybuckets.foundation.modelInterface;

import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public interface IMangedChunkData  {

    /**
     * Initialize the ManagedChunk and underlying data from memory or
     * perform any necessary configuration
     */
    boolean isInit(String subclass);

    /**
     * Instance method to force implementation on subclasses. resolveSubDataInstances
     * passes the burden of resolving the in-memory instance of subdata vs. the deserialized data
     * to the subclass itself.
     *
     * @param level - level this chunk was loaded in
     * @param id - string id of the chunk data to resolve in "x,z" format
     * @param serialized - the deserialized chunk data, if applicable
     * @return IMangedChunkData
     */
    @Nullable
    public IMangedChunkData resolveSubData(LevelAccessor level, String id, @Nullable IMangedChunkData serialized);

    /**
     * @param event
     */
    public void handleChunkLoaded(ChunkLoadingEvent.Load event);

    /**
     * Fired when a chunk is unloaded from memory
     * @param event
     */
    public void handleChunkUnloaded(ChunkLoadingEvent.Unload event);


    public CompoundTag serializeNBT();

    public void deserializeNBT(CompoundTag nbt);

    void setId(String id);

    void setLevel(LevelAccessor level);
}
