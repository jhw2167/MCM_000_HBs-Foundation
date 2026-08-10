package com.holybuckets.foundation.client.render;

// 26.1 PORT NOTE: This was a no-op BlockEntityRenderer for SimpleBlockEntity (the block renders
// via its baked model). The 26.1 BlockEntityRenderer contract moved to the render-state/submit
// pipeline (createRenderState + submit(..., CameraRenderState)), and the renderer drew nothing,
// so it is no longer registered (see ModRenderers) and reduced to this placeholder. Re-implement
// against the 26.1 BlockEntityRenderer API only if custom (non-model) rendering is needed.
public final class SimpleBlockEntityRenderer {
    private SimpleBlockEntityRenderer() {
    }
}
