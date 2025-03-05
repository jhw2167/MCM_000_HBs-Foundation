package com.holybuckets.foundation;

import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.minecraftforge.fml.common.Mod;

@Mod( Constants.MOD_ID)
public class FoundationMain {

    public FoundationMain() {
        super();
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        //DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initialize(Constants.MOD_ID, Client::initialize));
        LoggerBase.logInit( null, "001000", this.getClass().getName() );
    }

}
