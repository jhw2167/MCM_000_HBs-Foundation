package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.attachment.DataAttachmentLookup;
import net.blay09.mods.balm.platform.event.callback.LevelCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Description: ManagedChunk persistence via Balm's data attachment API.
 *
 * <p>Restructured to match the (working) NeoForge version — Balm's data attachment API is
 * cross-loader, so this file is identical on both loaders. Registration + the chunk-load hook happen
 * inside Balm's (bound) initializer via {@link #register(BalmRegistrars)}, replacing the old Fabric
 * {@code AttachmentRegistry.createPersistent} + {@code LevelCallback.Chunk.LOAD} listener that was
 * wired outside the bound init.
 *
 * <p>Design: the common {@link ManagedChunk} creation flow ({@code ManagedChunkEvents.onChunkLoad})
 * is unchanged — it still creates ManagedChunks into {@code ManagedChunkUtility}'s registry. Balm is
 * used purely for <b>persistence</b>: the created ManagedChunk is attached to the chunk so Balm saves
 * it, and Balm auto-decodes it back on load (whereupon the {@code ManagedChunk(CompoundTag)}
 * constructor re-registers it into the registry).
 */
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

    /**
     * Registers the Balm data attachment and the (now bound) chunk-load persistence hook.
     * MUST be called from within Balm's initializer (see FoundationAttachments#registerBalmAndEvents),
     * where the registrars and event mappings are available.
     */
    public static void register(BalmRegistrars registrars) {
        registrars.dataAttachmentTypes(r -> LOOKUP = r.register(ATTACHMENT_NAME, CODEC).asLookup());
        LevelCallback.Chunk.LOAD.register(ManagedChunkAttachment::onChunkLoad);
    }

    private static void onChunkLoad(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos) {
        if (LOOKUP == null || level.isClientSide()) return;
        // Persisted chunks are auto-decoded by Balm before this runs, so has() short-circuits them
        // (and the decode already re-registered the ManagedChunk into the registry).
        if (LOOKUP.has(chunk)) return;

        // Reuse the ManagedChunk the common flow created; create it if this hook happens to run
        // first (both paths dedupe via the LOADED_CHUNKS registry, so only one instance exists).
        ManagedChunk mc = ManagedChunkUtility.getInstance(level).getManagedChunk(chunkPos);
        if (mc == null) {
            mc = new ManagedChunk(level, chunkPos);
        }
        LOOKUP.update(chunk, mc);
    }
}
