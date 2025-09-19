package com.holybuckets.foundation.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class ControllerInputMessage {
    public static final String LOCATION = "controller_input";
    public final int controllerInput;
    public final int useDisplay;
    public final BlockPos pos;

    ControllerInputMessage(int controllerInput, int useDisplay, BlockPos pos) {
        this.controllerInput = controllerInput;
        this.useDisplay = useDisplay;
        this.pos = pos;
    }

    public static void createAndFire(int controllerInput, int useDisplay, BlockPos pos) {
        ControllerInputMessage message = new ControllerInputMessage(controllerInput, useDisplay, pos);
        ControllerInputMessageHandler.createAndFire(message);
    }
}
