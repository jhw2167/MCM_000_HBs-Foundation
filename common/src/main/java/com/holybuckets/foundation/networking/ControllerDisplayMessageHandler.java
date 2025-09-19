package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.world.entity.player.Player;

public class ControllerDisplayMessageHandler {
    public static String CLASS_ID = "017";

    public static void createAndFire(ControllerDisplayMessage message) {
        HBUtil.NetworkUtil.clientSendToServer(message);
    }

    public static void handle(Player player, ControllerDisplayMessage message) {
        // TODO: Implement display data handling logic
    }
}
