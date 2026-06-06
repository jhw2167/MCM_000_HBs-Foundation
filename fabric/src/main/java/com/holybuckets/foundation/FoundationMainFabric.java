package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import com.holybuckets.foundation.util.ModContext;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FoundationMainFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        FabricLoader.getInstance().getAllMods().forEach(container ->
            ModContext.getInstance().register(
                container.getMetadata().getId(),
                container.getMetadata().getName(),
                container.getMetadata().getVersion().getFriendlyString()
            )
        );

        Balm.initialize(Constants.MOD_ID, EmptyLoadContext.INSTANCE, CommonClass::init);

    }



}
