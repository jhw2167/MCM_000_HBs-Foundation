package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ClientInputEvent;
import com.holybuckets.foundation.LoggerBase;
import net.blay09.mods.balm.api.Balm;
import net.fabricmc.api.ModInitializer;


public class FoundationMain implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        FoundationAttachments.init();
        
        // Subscribe to client input events
        EventRegistrar.getInstance().registerOnClientInput(this::onClientInput);
    }

    private void onClientInput(ClientInputEvent event) {
        LoggerBase.logInfo(null, "001001", "Client Input Event - Keys pressed: " + event.getKeyCodes());

    }


}
