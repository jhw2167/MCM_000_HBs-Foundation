package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
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
        
        // For debugging purposes
        if (!message.keyCodes.isEmpty()) {
            String keys = message.keyCodes.stream()
                .map(code -> InputConstants.getKey(code, 0).getName())
                .collect(Collectors.joining(", "));
            //LoggerBase.logInfo(null, "015000", "Player " + player + " pressed keys: " + keys);
        }

        // Fire the client input event
        EventRegistrar.getInstance().onClientInput(message);
    }

}
