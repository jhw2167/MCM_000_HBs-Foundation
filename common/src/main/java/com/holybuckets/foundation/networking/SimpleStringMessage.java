package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Description: SimpleStringMessage
 * A simple message type for sending string data with a maximum size of 4096 characters
 */
public class SimpleStringMessage {

    public static final String LOCATION = "simple_string";
    public static final int MAX_SIZE = 4096;
    
    public final UUID senderId;
    public final String content;

    public SimpleStringMessage(UUID senderId, String content) {
        this.senderId = senderId;
        // Truncate content if it exceeds max size
        this.content = content != null && content.length() > MAX_SIZE 
            ? content.substring(0, MAX_SIZE) 
            : (content != null ? content : "");
    }

    public static SimpleStringMessage create(UUID senderId, String content) {
        return new SimpleStringMessage(senderId, content);
    }


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


}
