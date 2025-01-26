package com.holybuckets.foundation;

import com.holybuckets.foundation.model.ManagedChunk;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;


public class FoundationMain implements ModInitializer {
    
    @Override
    public void onInitialize() {
        
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.


        // Use Fabric to bootstrap the Common mod.
        CommonClass.init();

        // Register the attachment type
        BalmEvents events = Balm.getEvents();
        events.onEvent(ChunkLoadingEvent.Load.class, this::onChunkLoadRegisterAttachment);
    }

    final AttachmentType<ManagedChunk> MANAGED_CHUNK_ATTACHMENT =  AttachmentRegistry.createPersistent(
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
    private void onChunkLoadRegisterAttachment(ChunkLoadingEvent.Load event) {
        if(event.getLevel().isClientSide()) return;
        ChunkAccess chunk = event.getChunk();

        if(chunk.hasAttached(MANAGED_CHUNK_ATTACHMENT)) return;
        chunk.setAttached(MANAGED_CHUNK_ATTACHMENT, new ManagedChunk(event.getLevel(), event.getChunkPos() ));
    }
}
