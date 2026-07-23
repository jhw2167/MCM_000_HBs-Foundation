package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.model.ManagedChunk;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.holybuckets.foundation.event.balm.ChunkLoadingEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

/**
 * Description: Attach serializing/datastorage capabilities to a chunk (NeoForge)
 */
public class ManagedChunkAttachment {

    // Dummy method to prompt static initialization
    static void init() {}

    static final Supplier<AttachmentType<ManagedChunk>> MANAGED_CHUNK_ATTACHMENT =
        FoundationAttachments.ATTACHMENT_TYPES.register("managed_chunk",
            () -> AttachmentType.builder(() -> (ManagedChunk) null)
                .serialize(new Codec<>() {
                    @Override
                    public <T> DataResult<T> encode(ManagedChunk input, DynamicOps<T> ops, T prefix) {
                        CompoundTag tag = input.serializeNBT();
                        T converted = NbtOps.INSTANCE.convertTo(ops, tag);
                        return DataResult.success(converted);
                    }

                    @Override
                    public <T> DataResult<Pair<ManagedChunk, T>> decode(DynamicOps<T> ops, T input) {
                        if (input instanceof CompoundTag tag) {
                            return DataResult.success(Pair.of(new ManagedChunk(tag), ops.empty()));
                        }
                        return DataResult.error(() -> "Not an NBT tag");
                    }
                })
                .build()
        );

    static void onChunkLoadRegisterAttachment(ChunkLoadingEvent.Load event) {
        ChunkAccess chunk = event.getChunk();

        if (chunk.hasData(MANAGED_CHUNK_ATTACHMENT)) return;
        chunk.setData(MANAGED_CHUNK_ATTACHMENT, new ManagedChunk(event.getLevel(), event.getChunkPos()));
    }
}
