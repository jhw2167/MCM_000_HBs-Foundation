package com.holybuckets.foundation;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ClientInputEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod( Constants.MOD_ID)
public class FoundationMain {

    public FoundationMain() {
        super();
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initialize(Constants.MOD_ID, CommonClass::initClient));
        
        LoggerBase.logInit( null, "001000", this.getClass().getName() );
    }

    private void onClientInput(ClientInputEvent event) {
        LoggerBase.logInfo(null, "001001", "Client Input Event - Keys pressed: " + event.getKeyCodes());
    }

}
