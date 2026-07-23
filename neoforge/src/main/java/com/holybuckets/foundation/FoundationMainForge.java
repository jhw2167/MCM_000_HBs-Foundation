package com.holybuckets.foundation;

import com.holybuckets.foundation.capability.FoundationAttachments;
import com.holybuckets.foundation.util.ModContext;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class FoundationMainForge {

    public FoundationMainForge(ModContainer modContainer, IEventBus modEventBus) {
        super();
        // Register NeoForge attachment types to the mod event bus

        ModList.get().getMods().forEach(info ->
            ModContext.getInstance().register(
                info.getModId(),
                info.getDisplayName(),
                info.getVersion().toString()
            )
        );
        final var context = new NeoForgeLoadContext(modContainer, modEventBus);
        FoundationAttachments.init();
        FoundationAttachments.ATTACHMENT_TYPES.register(modEventBus);
        Balm.initializeMod(Constants.MOD_ID, context, CommonClass::init);
    }

}
