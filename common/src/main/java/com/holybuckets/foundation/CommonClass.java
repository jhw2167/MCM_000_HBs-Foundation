package com.holybuckets.foundation;

import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.item.WaypointStick;
import com.holybuckets.foundation.platform.Services;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;


public class CommonClass {

    public static boolean isInitialized = false;
    public static IMessager MESSAGER;

    public static void init()
    {
        Constants.LOG.info("Loaded {} mod on {}! we are currently in a {} environment!", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        FoundationInitializers.init();
        EventRegistrar reg = EventRegistrar.getInstance();
        WaypointStick.init(reg);
        // Debug-only hooks live in CommonClassDebug. Activate individual hooks by
        // uncommenting registrations inside that class.
        CommonClassDebug.init(reg);

        BalmEventRegister.registerEvents();
        isInitialized = true;
    }

    //ANYTHING HERE SHOULD BE HIGH PRIORITY, DO FROM GENERAL CONFIG
    private static void onServerStarting(ServerStartingEvent event) {

    }

}
