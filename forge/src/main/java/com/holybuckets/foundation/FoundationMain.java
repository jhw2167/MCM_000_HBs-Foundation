package com.holybuckets.foundation;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraftforge.fml.common.Mod;

@Mod( Constants.MOD_ID)
public class FoundationMain {

    public FoundationMain() {
        super();
        CommonClass.init();
        LoggerBase.logInit( null, "001000", this.getClass().getName() );
    }

}
