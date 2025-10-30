package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
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

        public static void handle(Player player, SimpleStringMessage message)
        {
            EventRegistrar.getInstance().onSimpleStringMessage(player, message);
        }
    }


}
