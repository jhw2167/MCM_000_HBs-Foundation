package com.holybuckets.foundation.client;

import com.holybuckets.foundation.block.entity.ModBlockEntities;
import com.holybuckets.foundation.client.render.SimpleBlockEntityRenderer;
import net.blay09.mods.balm.client.renderer.blockentity.BalmBlockEntityRendererRegistrar;

public class ModRenderers {

    public static void clientInitialize(BalmBlockEntityRendererRegistrar renderers) {
        renderers.register(ModBlockEntities.simpleBlockEntityType, SimpleBlockEntityRenderer::new);
    }

}
