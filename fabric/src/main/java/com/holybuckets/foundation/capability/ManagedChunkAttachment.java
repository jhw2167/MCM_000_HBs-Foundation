package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.model.ManagedChunk;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Description: Attach serializing/datastorage capabilities to a chunk
 */
public class ManagedChunkAttachment {

    //Dummy method to prompt static initialization
    static void init() {}

    static final AttachmentType<ManagedChunk> MANAGED_CHUNK_ATTACHMENT =  AttachmentRegistry.createPersistent(
        new ResourceLocation(Constants.MOD_ID, "managed_chunk"),
        new Codec<>() {

            @Override
            public <T> DataResult<T> encode(ManagedChunk input, DynamicOps<T> ops, T prefix) {
                CompoundTag tag = input.serializeNBT();
                T converted = NbtOps.INSTANCE.convertTo(ops, tag);
                return DataResult.success(converted);
            }

            @Override
            public <T> DataResult<Pair<ManagedChunk, T>> decode(DynamicOps<T> ops, T input) {
                if(ops instanceof NbtOps)
                {
                    // If the ops are already NbtOps, we can safely cast the input
                    CompoundTag tag = (CompoundTag) input;
                    return DataResult.success(Pair.of( new ManagedChunk(tag), ops.empty()));
                }

                return DataResult.error(() -> "Not an NBT tag");
            }
        });

    static void onChunkLoadRegisterAttachment(ChunkLoadingEvent.Load event) {
        ChunkAccess chunk = event.getChunk();

        if(chunk.hasAttached(MANAGED_CHUNK_ATTACHMENT)) return;
        chunk.setAttached(MANAGED_CHUNK_ATTACHMENT, new ManagedChunk(event.getLevel(), event.getChunkPos() ));
    }

}
