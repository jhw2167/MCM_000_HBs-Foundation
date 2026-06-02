package com.holybuckets.foundation;

import com.holybuckets.foundation.client.CommonClassClient;
import com.holybuckets.foundation.util.ModContext;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
/*
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
 */

@Mod( Constants.MOD_ID)
public class FoundationMainForge {

    public FoundationMainForge() {
        super();

        ModList.get().getMods().forEach(info ->
            ModContext.getInstance().register(
                info.getModId(),
                info.getDisplayName(),
                info.getVersion().toString()
            )
        );

        Balm.initialize(Constants.MOD_ID, CommonClass::init);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient));
    }

}
