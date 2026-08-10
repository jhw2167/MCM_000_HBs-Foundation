package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;

public class FoundationAttachments {

    public static void init() {
        ManagedPlayerAttachment.init();
    }

    private static void registerAttachments() {
        BalmEvents events = Balm.getEvents();
        events.onEvent(ChunkLoadingEvent.Load.class, ManagedChunkAttachment::onChunkLoadRegisterAttachment);
        //events.onEvent(PlayerLoginEvent.class, ManagedPlayerAttachment::onPlayerLoginRegisterAttachment, EventPriority.Highest);
        // Player attachment stays Fabric-native (AttachmentRegistry); only its trigger lives here.
        ServerPlayerCallback.Join.EVENT.register(EventPriority.Highest.toPhase(), player ->
            ManagedPlayerAttachment.onPlayerLoginRegisterAttachment(new PlayerLoginEvent(player)));
    }



}
