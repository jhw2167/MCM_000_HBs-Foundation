package com.holybuckets.foundation.networking;

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
}
