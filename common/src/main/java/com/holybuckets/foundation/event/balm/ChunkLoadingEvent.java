package com.holybuckets.foundation.event.balm;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public abstract class ChunkLoadingEvent extends BalmEvent {
    private final LevelAccessor level;
    private final ChunkAccess chunk;
    private final ChunkPos chunkPos;

    public ChunkLoadingEvent(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos) {
        this.level = level;
        this.chunk = chunk;
        this.chunkPos = chunkPos;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    public ChunkAccess getChunk() {
        return chunk;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public static class Load extends ChunkLoadingEvent {
        public Load(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos) {
            super(level, chunk, chunkPos);
        }
    }

    public static class Unload extends ChunkLoadingEvent {
        public Unload(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos) {
            super(level, chunk, chunkPos);
        }
    }
}
