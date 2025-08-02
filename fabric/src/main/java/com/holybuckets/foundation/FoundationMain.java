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
        

    }



}
