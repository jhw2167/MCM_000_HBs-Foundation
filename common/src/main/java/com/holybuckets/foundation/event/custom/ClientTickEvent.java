package com.holybuckets.foundation.event.custom;

public class ClientTickEvent {
    private final long tickCount;

    public ClientTickEvent(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getTickCount() {
        return tickCount;
    }

    public static class SingleTick extends ClientTickEvent {
        public SingleTick(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every20Ticks extends ClientTickEvent {
        public Every20Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every120Ticks extends ClientTickEvent {
        public Every120Ticks(long tickCount) {
            super(tickCount);
        }
    }

    public static class Every1200Ticks extends ClientTickEvent {
        public Every1200Ticks(long tickCount) {
            super(tickCount);
        }
    }
}
