package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.event.balm.ChunkLoadingEvent;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.blay09.mods.balm.platform.event.callback.LevelCallback;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class FoundationAttachments {

    // Deferred register for attachment types — must be registered to the mod event bus
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static void init() {
        initAttachments();
        registerAttachments();
    }

    private static void initAttachments() {
        ManagedChunkAttachment.init();
        ManagedPlayerAttachment.init();
    }

    private static void registerAttachments() {
        LevelCallback.Chunk.LOAD.register((level, chunk, chunkPos) ->
            ManagedChunkAttachment.onChunkLoadRegisterAttachment(new ChunkLoadingEvent.Load(level, chunk, chunkPos)));
        ServerPlayerCallback.Join.EVENT.register(EventPriority.Highest.toPhase(), player ->
            ManagedPlayerAttachment.onPlayerLoginRegisterAttachment(new PlayerLoginEvent(player)));
    }
}
