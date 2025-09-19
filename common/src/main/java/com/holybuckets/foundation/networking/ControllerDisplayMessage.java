package com.holybuckets.foundation.networking;

import net.minecraft.core.BlockPos;

public class ControllerDisplayMessage {
    public static final String LOCATION = "controller_display";
    public final BlockPos pos;
    public final int[] displayData;

    ControllerDisplayMessage(BlockPos pos, int[] displayData) {
        this.pos = pos;
        this.displayData = displayData;
    }

    public static void createAndFire(BlockPos pos, int[] displayData) {
        if (displayData.length != 4096) {
            throw new IllegalArgumentException("Display data must be exactly 4096 integers");
        }
        ControllerDisplayMessage message = new ControllerDisplayMessage(pos, displayData);
        ControllerDisplayMessageHandler.createAndFire(message);
    }
}
