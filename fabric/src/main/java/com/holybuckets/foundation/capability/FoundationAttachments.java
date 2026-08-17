package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.core.BalmRegistrars;

public class FoundationAttachments {

    public static void init() {
        ManagedPlayerAttachment.init();
    }

    public static void registerBalmAndEvents(BalmRegistrars registrars) {
        ManagedChunkAttachment.register(registrars);
        ManagedPlayerAttachment.register(registrars);
    }



}
