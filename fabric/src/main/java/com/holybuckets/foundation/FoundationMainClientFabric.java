package com.holybuckets.foundation;

import com.holybuckets.foundation.client.CommonClassClient;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;


public class FoundationMainClientFabric implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(Constants.MOD_ID, FabricLoadContext.INSTANCE, CommonClassClient::initClient);
        // 26.1.2: currently a no-op — Fabric API removed WorldRenderEvents in 1.21.9 (see
        // FoundationRenderEvents). In-world render stages don't fire on Fabric until a mixin is added.
        com.holybuckets.foundation.client.FoundationRenderEvents.init();
    }

}
