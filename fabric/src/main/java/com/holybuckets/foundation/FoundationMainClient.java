package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;


public class FoundationMainClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, CommonClass::initClient);
    }

}
