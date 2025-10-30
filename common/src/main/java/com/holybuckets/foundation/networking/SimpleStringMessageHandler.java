package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.entity.player.Player;

public class SimpleStringMessageHandler {

    public static String CLASS_ID = "016";

    public static void handle(Player player, SimpleStringMessage message) {
        // Validate that the message is from the correct player
        if (!player.getUUID().equals(message.senderId)) {
            LoggerBase.logError(null, "016001", "Received string message from player " + message.senderId + " but expected " + player.getUUID());
            return;
        }

        // Validate content size
        if (message.content.length() > SimpleStringMessage.MAX_SIZE) {
            LoggerBase.logError(null, "016002", "Received string message exceeding max size: " + message.content.length() + " > " + SimpleStringMessage.MAX_SIZE);
            return;
        }

        // Log the received message for now - in a real implementation you'd probably fire an event
        LoggerBase.logInfo(null, "016003", "Received string message from player " + player.getName().getString() + ": " + message.content);
    }
}
