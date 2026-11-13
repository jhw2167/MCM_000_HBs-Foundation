package com.holybuckets.foundation.client.overlay;

import java.util.function.BooleanSupplier;

public record ClientOverlayInfo(OverlayPriority priority, OverlayDrawer drawer, BooleanSupplier visible) {
}
