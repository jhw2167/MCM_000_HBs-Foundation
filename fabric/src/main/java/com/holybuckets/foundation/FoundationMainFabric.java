package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;

public class FoundationMainFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Balm.initialize(Constants.MOD_ID, EmptyLoadContext.INSTANCE, CommonClass::init);
        FoundationAttachments.init();
    }



}
