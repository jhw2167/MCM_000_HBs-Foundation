package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class FoundationAttachments {

    // Deferred register for the (still NeoForge-native) player attachment type — registered to the
    // mod event bus in FoundationMainForge. The chunk attachment now goes through Balm's data
    // attachment API instead (see ManagedChunkAttachment).
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    /**
     * Called from the {@code @Mod} constructor. Only static-inits the NeoForge attachment type(s)
     * so they are added to {@link #ATTACHMENT_TYPES} before it is registered to the mod event bus.
     * Balm-dependent registration and event callbacks must NOT run here — Balm has not yet bound its
     * event mappings, which previously caused "LevelCallback.Chunk.LOAD unbound".
     */
    public static void init() {
        ManagedPlayerAttachment.init();
    }

    /**
     * Called from within Balm's initializer (see FoundationMainForge), where the registrars and
     * event mappings are available/bound. Registers the Balm chunk data attachment and the
     * player-join trigger.
     */
    public static void registerBalmAndEvents(BalmRegistrars registrars) {
        // ManagedChunk persistence via Balm's data attachment API.
        ManagedChunkAttachment.register(registrars);

        // Player attachment stays NeoForge-native; only its (previously unbound) trigger moves here.
        ServerPlayerCallback.Join.EVENT.register(EventPriority.Highest.toPhase(), player ->
            ManagedPlayerAttachment.onPlayerLoginRegisterAttachment(new PlayerLoginEvent(player)));
    }
}
