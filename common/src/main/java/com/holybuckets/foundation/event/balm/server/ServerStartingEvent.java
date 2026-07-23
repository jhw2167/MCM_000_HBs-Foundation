package com.holybuckets.foundation.event.balm.server;

import com.holybuckets.foundation.event.balm.BalmEvent;
import net.minecraft.server.MinecraftServer;

public class ServerStartingEvent extends BalmEvent {
    private final MinecraftServer server;

    public ServerStartingEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
