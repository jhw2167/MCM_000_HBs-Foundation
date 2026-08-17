package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class FoundationAttachments {

    // Deferred register for attachment types — must be registered to the mod event bus
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static void init() {
        ManagedPlayerAttachment.init();
    }


    public static void registerBalmAndEvents(BalmRegistrars registrars) {
        ManagedChunkAttachment.register(registrars);
        ManagedPlayerAttachment.register(registrars);
    }
}
