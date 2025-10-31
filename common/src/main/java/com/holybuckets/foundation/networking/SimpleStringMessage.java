package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
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
    public final String messageId;
    public final String content;

    public SimpleStringMessage(UUID senderId, String messageId, String content) {
        this.senderId = senderId;
        this.messageId = messageId != null ? messageId : "default";
        this.content = content != null && content.length() > MAX_SIZE 
            ? content.substring(0, MAX_SIZE) 
            : (content != null ? content : "");
    }

    public SimpleStringMessage(UUID senderId, String content) {
        this(senderId, "default", content);
    }

    public static SimpleStringMessage createAndFire(Player p, String messageId, String content) {
        SimpleStringMessage message = new SimpleStringMessage(p.getUUID(), messageId, content);
        if (GeneralConfig.getInstance().isServerSide()) {
            HBUtil.NetworkUtil.serverSendToPlayer(p, message);
        } else {
            HBUtil.NetworkUtil.clientSendToServer(message);
        }
        return message;
    }

    public static SimpleStringMessage createAndFire(Player p, String content) {
        return createAndFire(p, "default", content);
    }

    public static class SimpleStringMessageHandler {

        public static String CLASS_ID = "016";

        public static void handle(Player player, SimpleStringMessage message) {
            // Fire the simple message event with messageId routing
            if (GeneralConfig.getInstance().isServerSide()) {
                EventRegistrar.getInstance().onSimpleMessage(player, message, message.messageId);
            } else {
                ClientEventRegistrar.getInstance().onSimpleMessage(player, message, message.messageId);
            }
        }
    }
}
