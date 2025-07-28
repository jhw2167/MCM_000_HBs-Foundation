package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.player.Player;

public class ClientInputMessageHandler {

    public static String CLASS_ID = "015";

    public static void handle(Player player, ClientInputMessage message) {
        // Validate that the message is from the correct player
        if (!player.getUUID().equals(message.playerId)) {
            LoggerBase.logError(null, "015002", "Received input message from player " + message.playerId + " but expected " + player.getUUID());
            return;
        }
        
        // Fire the client input event
        EventRegistrar.getInstance().onClientInput(player, message);
        
        String key = InputConstants.getKey(0, message.code).getName();
        LoggerBase.logInfo(null, "015000",  "Player " + player + " pressed " + key);
    }

}
