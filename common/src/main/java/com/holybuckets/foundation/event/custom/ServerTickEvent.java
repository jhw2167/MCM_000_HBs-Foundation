package com.holybuckets.foundation.event.custom;

import net.minecraft.world.level.Level;

public class ServerTickEvent {
    private final long tickCount;

    public ServerTickEvent(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getTickCount() {
        return tickCount;
    }

    public static class SingleTick extends ServerTickEvent {
        public SingleTick(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every20Ticks extends ServerTickEvent {
        public Every20Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every120Ticks extends ServerTickEvent {
        public Every120Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every1200Ticks extends ServerTickEvent {
        public Every1200Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class DailyTickEvent extends ServerTickEvent {
        private Level level;
        private boolean triggeredByWakeUp;
        private long tickCountWithSleeps;
        public DailyTickEvent(long tickCount, long tickCountWithSleeps, Level level, boolean triggeredByWakeUp) {
            super(tickCount);
            this.tickCountWithSleeps = tickCountWithSleeps;
            this.level = level;
            this.triggeredByWakeUp = triggeredByWakeUp;
        }

        public long getTickCountWithSleeps() {
            return tickCountWithSleeps;
        }

        public boolean isTriggeredByWakeUp() {
            return triggeredByWakeUp;
        }

        public Level getLevel() {
            return level;
        }
    }
}
