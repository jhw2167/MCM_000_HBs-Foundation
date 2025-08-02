package com.holybuckets.foundation.event.custom;

import net.minecraft.client.Minecraft;

public class ClientTickEvent {
    private final long tickCount;

    public ClientTickEvent(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getTickCount() {
        return tickCount;
    }

}
