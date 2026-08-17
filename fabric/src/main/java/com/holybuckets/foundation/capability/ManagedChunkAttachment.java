package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.attachment.DataAttachmentLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;


public class ManagedChunkAttachment {

    private static final String ATTACHMENT_NAME = "managed_chunk";

    private static DataAttachmentLookup<ManagedChunk> LOOKUP;

    // Retained for API parity / callers that referenced it; no longer performs registration.
    static void init() {}

    static final Codec<ManagedChunk> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(ManagedChunk input, DynamicOps<T> ops, T prefix) {
            CompoundTag tag = input.serializeNBT();
            return DataResult.success(NbtOps.INSTANCE.convertTo(ops, tag));
        }

        @Override
        public <T> DataResult<Pair<ManagedChunk, T>> decode(DynamicOps<T> ops, T input) {
            // Ops-agnostic: convert whatever dynamic form Balm hands us into an NBT CompoundTag.
            CompoundTag tag = ops.convertTo(NbtOps.INSTANCE, input) instanceof CompoundTag c ? c : null;
            if (tag == null) {
                return DataResult.error(() -> "ManagedChunk attachment: expected a CompoundTag");
            }
            return DataResult.success(Pair.of(new ManagedChunk(tag), ops.empty()));
        }
    };


    public static void register(BalmRegistrars registrars) {
        registrars.dataAttachmentTypes(r -> LOOKUP = r.register(ATTACHMENT_NAME, CODEC).asLookup());
        Balm.getEvents().onEvent(ChunkLoadingEvent.Load.class, ManagedChunkAttachment::onChunkLoad, EventPriority.Highest);
    }

    private static void onChunkLoad(ChunkLoadingEvent event) {
        LevelAccessor level = event.getLevel();
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        onChunkLoad(level, chunk, chunkPos);
    }

    private static void onChunkLoad(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos) {
        if (LOOKUP == null || level.isClientSide()) return;
        if (LOOKUP.has(chunk)) return;

        ManagedChunk mc = ManagedChunkUtility.getInstance(level).getManagedChunk(chunkPos);
        if (mc == null) {
            mc = new ManagedChunk(level, chunkPos);
        }
        LOOKUP.update(chunk, mc);
    }
}
