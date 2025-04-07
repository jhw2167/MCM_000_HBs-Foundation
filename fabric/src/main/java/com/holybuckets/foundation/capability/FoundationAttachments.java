package com.holybuckets.foundation.capability;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;

public class FoundationAttachments {

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
        events.onEvent(PlayerConnectedEvent.class, ManagedPlayerAttachment::onPlayerConnectedRegisterAttachment);
    }


}
