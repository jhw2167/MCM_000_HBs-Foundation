package com.holybuckets.foundation.event.custom;

public enum TickType {
    ON_SINGLE_TICK,
    ON_20_TICKS,
    ON_120_TICKS,
    ON_1200_TICKS,
    ON_6000_TICKS,
    ON_24000_TICKS,
    DAILY_TICK  /**24000 ticks OR wakeup event**/
}
