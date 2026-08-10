package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;

public class FoundationAttachments {

    /**
     * Called from {@code FoundationMainFabric#onInitialize}. Only static-inits the Fabric-native
     * player attachment type (registered via Fabric's AttachmentRegistry when the class loads).
     * Balm-dependent registration and event callbacks must NOT run here — Balm has not yet bound its
     * event mappings, which would cause "LevelCallback.Chunk.LOAD unbound" (mirrors the NeoForge fix).
     */
    public static void init() {
        ManagedPlayerAttachment.init();
    }

    /**
     * Called from within Balm's initializer (see FoundationMainFabric), where the registrars and
     * event mappings are available/bound. Registers the Balm chunk data attachment and the
     * player-join trigger — identical structure to the NeoForge FoundationAttachments.
     */
    public static void registerBalmAndEvents(BalmRegistrars registrars) {
        // ManagedChunk persistence via Balm's data attachment API (cross-loader).
        ManagedChunkAttachment.register(registrars);

        // Player attachment stays Fabric-native (AttachmentRegistry); only its trigger lives here.
        ServerPlayerCallback.Join.EVENT.register(EventPriority.Highest.toPhase(), player ->
            ManagedPlayerAttachment.onPlayerLoginRegisterAttachment(new PlayerLoginEvent(player)));
    }
}
