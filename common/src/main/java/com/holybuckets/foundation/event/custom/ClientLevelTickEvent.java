package com.holybuckets.foundation.event.custom;

import net.minecraft.world.level.Level;

public class ClientLevelTickEvent {
    private final long tickCount;
    private final Level level;

    public ClientLevelTickEvent(Level level, long tickCount) {
        this.tickCount = tickCount;
        this.level = level;
    }

    public long getTickCount() {
        return tickCount;
    }

}
