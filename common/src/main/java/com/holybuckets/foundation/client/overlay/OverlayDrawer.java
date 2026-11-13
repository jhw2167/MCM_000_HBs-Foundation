package com.holybuckets.foundation.client.overlay;

import net.blay09.mods.balm.api.event.client.GuiDrawEvent;

@FunctionalInterface
public interface OverlayDrawer {

    void draw(GuiDrawEvent.Post event, OverlayPosition position);
}
