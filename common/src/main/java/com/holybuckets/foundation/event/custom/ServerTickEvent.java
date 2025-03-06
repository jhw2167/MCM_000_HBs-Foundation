package com.holybuckets.foundation.event.custom;

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

    public static class DailyTick extends ServerTickEvent {
        public DailyTick(long tickCount) {
            super(tickCount);
        }
    }
}
