package com.holybuckets.foundation.client;

import net.blay09.mods.balm.client.renderer.blockentity.BalmBlockEntityRendererRegistrar;

public class ModRenderers {

    // 26.1 PORT NOTE: SimpleBlockEntity had a no-op BlockEntityRenderer (the block renders via its
    // baked model). The 26.1 BlockEntityRenderer contract changed to the render-state/submit
    // pipeline (createRenderState + submit(..., CameraRenderState)); rather than port a renderer
    // that drew nothing, it is simply no longer registered. Re-add a renderer here only if
    // SimpleBlockEntity needs custom (non-model) rendering.
    public static void clientInitialize(BalmBlockEntityRendererRegistrar renderers) {
        // intentionally empty
    }

}
