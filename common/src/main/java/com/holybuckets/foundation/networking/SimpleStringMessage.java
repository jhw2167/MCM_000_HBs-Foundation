package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.client.ClientEventRegistrar;
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


    public static class SimpleStringMessageHandler {

        public static String CLASS_ID = "016";

        public static void handle(Player player, SimpleStringMessage message) {
            // Extract messageId from the beginning of the content if it exists
            String messageId = "default";
            String actualContent = message.content;
            
            if (message.content != null && message.content.startsWith("[") && message.content.contains("]")) {
                int endBracket = message.content.indexOf("]");
                if (endBracket > 1) {
                    messageId = message.content.substring(1, endBracket);
                    actualContent = message.content.substring(endBracket + 1);
                }
            }
            
            // Create a new message with the actual content (without messageId prefix)
            SimpleStringMessage processedMessage = new SimpleStringMessage(message.senderId, actualContent);
            
            // Fire the simple message event with messageId routing
            if (player.level().isClientSide) {
                ClientEventRegistrar.getInstance().onSimpleMessage(player, processedMessage, messageId);
            } else {
                EventRegistrar.getInstance().onSimpleMessage(player, processedMessage, messageId);
            }
        }
    }
}
