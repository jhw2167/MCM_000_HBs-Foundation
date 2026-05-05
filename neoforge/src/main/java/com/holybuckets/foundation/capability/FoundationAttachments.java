package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
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
        BalmEvents events = Balm.getEvents();
        events.onEvent(ChunkLoadingEvent.Load.class, ManagedChunkAttachment::onChunkLoadRegisterAttachment);
        events.onEvent(PlayerLoginEvent.class, ManagedPlayerAttachment::onPlayerLoginRegisterAttachment, EventPriority.Highest);
    }
}
