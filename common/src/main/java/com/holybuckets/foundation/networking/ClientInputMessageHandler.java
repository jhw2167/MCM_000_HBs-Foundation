package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.entity.player.Player;

public class ClientInputMessageHandler {
    public static void handle(Player player, ClientInputMessage message) {
        // Validate that the message is from the correct player
        if (!player.getUUID().equals(message.playerId)) {
            LoggerBase.warn("Received client input message from wrong player!");
            return;
        }

        // Handle the input
        switch (message.inputType) {
            case "KEY":
                handleKeyPress(player, message.code);
                break;
            case "MOUSE":
                handleMouseClick(player, message.code);
                break;
        }
    }

    private static void handleKeyPress(Player player, int keyCode) {
        // Handle key press on server side
        LoggerBase.info("Player " + player.getName().getString() + " pressed key: " + keyCode);
    }

    private static void handleMouseClick(Player player, int button) {
        // Handle mouse click on server side
        LoggerBase.info("Player " + player.getName().getString() + " clicked button: " + button);
    }
}
