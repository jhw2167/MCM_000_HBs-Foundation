package com.holybuckets.foundation.event.balm;

import net.blay09.mods.balm.platform.event.EventPhases;
import net.minecraft.resources.Identifier;

public enum EventPriority {
    Lowest,
    Low,
    Normal,
    High,
    Highest;

    public static EventPriority[] values = EventPriority.values();

    public Identifier toPhase() {
        switch (this) {
            case Lowest: return EventPhases.LOWEST;
            case Low: return EventPhases.LOW;
            case High: return EventPhases.HIGH;
            case Highest: return EventPhases.HIGHEST;
            default: return EventPhases.DEFAULT;
        }
    }
}
