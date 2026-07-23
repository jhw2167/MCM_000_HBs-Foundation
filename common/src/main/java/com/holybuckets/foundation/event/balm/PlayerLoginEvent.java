package com.holybuckets.foundation.event.balm;

import net.minecraft.server.level.ServerPlayer;

public class PlayerLoginEvent extends BalmEvent {
    private final ServerPlayer player;

    public PlayerLoginEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
