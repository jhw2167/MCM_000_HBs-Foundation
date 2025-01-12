package com.holybuckets.foundation;

import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;


public class CommonClass {

    public static void init()
    {

        Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        if (Services.PLATFORM.isModLoaded("examplemod")) {

            Constants.LOG.info("Hello to examplemod");
        }

        EventRegistrar.init();
        EventRegistrar reg = EventRegistrar.getInstance();

        DataStore.init(reg);

    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }

}