package com.holybuckets.foundation.event.custom;

import net.minecraft.client.Minecraft;

public class ClientTickEvent {
    private final long tickCount;
    private final Minecraft client;

    public ClientTickEvent(Minecraft client, long tickCount) {
        this.client = client;
        this.tickCount = tickCount;
    }

    public long getTickCount() {
        return tickCount;
    }

}
