package com.holybuckets.foundation.event.balm.client;

import com.holybuckets.foundation.event.balm.BalmEvent;
import net.minecraft.client.Minecraft;

public class ConnectedToServerEvent extends BalmEvent {
    private final Minecraft client;

    public ConnectedToServerEvent(Minecraft client) {
        this.client = client;
    }

    public Minecraft getClient() {
        return client;
    }
}
