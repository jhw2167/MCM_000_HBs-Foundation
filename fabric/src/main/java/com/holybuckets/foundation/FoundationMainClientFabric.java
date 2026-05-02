package com.holybuckets.foundation;

import com.holybuckets.foundation.client.CommonClassClient;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.fabric.FabricBalmRuntime;
import net.fabricmc.api.ClientModInitializer;


public class FoundationMainClientFabric implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, EmptyLoadContext.INSTANCE, CommonClassClient::initClient);
    }

}
