package com.holybuckets.foundation.event.custom;

import net.minecraft.server.level.ServerLevel;

public class WakeUpAllPlayersEvent {
    private final ServerLevel level;
    private final int totalSleeps;

    public WakeUpAllPlayersEvent(ServerLevel level, int totalSleeps) {
        this.level = level;
        this.totalSleeps = totalSleeps;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public int getTotalSleeps() {
        return totalSleeps;
    }

    public long getDimensionTicks() {
        return level.getGameTime();
    }
}
