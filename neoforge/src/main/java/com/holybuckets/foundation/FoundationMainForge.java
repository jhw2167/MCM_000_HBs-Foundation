package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import com.holybuckets.foundation.client.CommonClassClient;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class FoundationMainForge {

    public FoundationMainForge(IEventBus modEventBus) {
        super();
        // Register NeoForge attachment types to the mod event bus
        FoundationAttachments.ATTACHMENT_TYPES.register(modEventBus);

        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient));

        FoundationAttachments.init();
    }

}
