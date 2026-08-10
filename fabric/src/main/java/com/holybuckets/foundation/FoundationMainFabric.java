package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import com.holybuckets.foundation.util.ModContext;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
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

        // Mirror the NeoForge structure: static-init the Fabric-native player attachment type first,
        // then run Balm-bound registration/callbacks inside the initializer (event mappings are bound
        // there — registering them outside previously risked "LevelCallback.Chunk.LOAD unbound").
        FoundationAttachments.init();
        Balm.initializeMod(Constants.MOD_ID, FabricLoadContext.INSTANCE, registrars -> {
            CommonClass.init(registrars);
            FoundationAttachments.registerBalmAndEvents(registrars);
        });
    }



}
