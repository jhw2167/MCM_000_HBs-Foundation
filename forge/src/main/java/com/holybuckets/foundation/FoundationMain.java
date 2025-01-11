package com.holybuckets.foundation;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraftforge.fml.common.Mod;

@Mod( FoundationMain.MOD_ID)
public class FoundationMain {

    public static final String MOD_ID = "hbs_foundation";
    public static final String NAME = "HBs Foundation";
    public static final String VERSION = "1.0.0f";

    public static EventRegistrar eventRegistrar = EventRegistrar.getInstance();

    public FoundationMain() {
        super();
        LoggerBase.logInit( null, "001000", this.getClass().getName() );
    }

    public static void initMod() {
        LoggerBase.logInit( null, "001001", "Initializing Foundation" );
    }
}
