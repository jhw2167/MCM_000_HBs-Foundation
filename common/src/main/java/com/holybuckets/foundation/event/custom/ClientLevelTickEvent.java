package com.holybuckets.foundation.event.custom;

public class ClientLevelTickEvent {
    private final long tickCount;

    public ClientLevelTickEvent(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getTickCount() {
        return tickCount;
    }

    public static class SingleTick extends ClientLevelTickEvent {
        public SingleTick(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every20Ticks extends ClientLevelTickEvent {
        public Every20Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every120Ticks extends ClientLevelTickEvent {
        public Every120Ticks(long tickCount) {
            super(tickCount);
        }
    }
}
