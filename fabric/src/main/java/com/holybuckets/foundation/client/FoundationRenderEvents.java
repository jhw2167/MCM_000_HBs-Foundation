package com.holybuckets.foundation.client;


public final class FoundationRenderEvents {

    private FoundationRenderEvents() {
    }

    public static void init() {
        // No-op: Fabric world-render events were removed in 1.21.9 (see class doc). Wire a mixin here
        // if/when in-world rendering is needed on Fabric.
    }
}
