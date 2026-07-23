package com.holybuckets.foundation.event.balm.client;

import com.holybuckets.foundation.event.balm.BalmEvent;
import net.minecraft.client.Minecraft;

public class ClientStartedEvent extends BalmEvent {
    private final Minecraft minecraft;

    public ClientStartedEvent(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }
}
