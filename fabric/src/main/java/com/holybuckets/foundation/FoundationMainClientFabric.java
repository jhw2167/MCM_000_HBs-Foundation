package com.holybuckets.foundation;

import com.holybuckets.foundation.client.CommonClassClient;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;


public class FoundationMainClientFabric implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(Constants.MOD_ID, FabricLoadContext.INSTANCE, CommonClassClient::initClient);
    }

}
