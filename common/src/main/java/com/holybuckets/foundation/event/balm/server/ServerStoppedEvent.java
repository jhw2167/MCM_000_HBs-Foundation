package com.holybuckets.foundation.event.balm.server;

import com.holybuckets.foundation.event.balm.BalmEvent;
import net.minecraft.server.MinecraftServer;

public class ServerStoppedEvent extends BalmEvent {
    private final MinecraftServer server;

    public ServerStoppedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public MinecraftServer server() {
        return server;
    }
}
