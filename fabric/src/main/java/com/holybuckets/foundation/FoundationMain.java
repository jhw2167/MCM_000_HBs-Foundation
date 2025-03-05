package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import net.blay09.mods.balm.api.Balm;
import net.fabricmc.api.ModInitializer;


public class FoundationMain implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        FoundationAttachments.init();

    }


}
