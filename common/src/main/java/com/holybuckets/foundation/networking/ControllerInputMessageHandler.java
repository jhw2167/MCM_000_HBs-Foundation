package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.world.entity.player.Player;

public class ControllerInputMessageHandler {
    public static String CLASS_ID = "016";

    public static void createAndFire(ControllerInputMessage message) {
        HBUtil.NetworkUtil.clientSendToServer(message);
    }

    public static void handle(Player player, ControllerInputMessage message) {
        // TODO: Implement controller input handling logic
    }
}
