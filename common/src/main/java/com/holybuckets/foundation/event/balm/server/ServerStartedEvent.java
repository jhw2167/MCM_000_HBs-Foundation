package com.holybuckets.foundation.event.balm.server;

import com.holybuckets.foundation.event.balm.BalmEvent;
import net.minecraft.server.MinecraftServer;

public class ServerStartedEvent extends BalmEvent {
    private final MinecraftServer server;

    public ServerStartedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
