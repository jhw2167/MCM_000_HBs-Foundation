package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Description: SimpleStringMessage
 * A simple message type for sending string data with a maximum size of 4096 characters
 */
public class SimpleStringMessage {

    public static final String LOCATION = "simple_string";
    public static final int MAX_SIZE = 4096;

    @Nullable
    public final UUID senderId;
    public final String messageId;
    public final String content;

    public SimpleStringMessage(@Nullable UUID senderId, String messageId, String content) {
        this.senderId = senderId;
        this.messageId = messageId != null ? messageId : "default";
        this.content = content != null && content.length() > MAX_SIZE 
            ? content.substring(0, MAX_SIZE) 
            : (content != null ? content : "");
    }

    public SimpleStringMessage(String messageId, String content) {
        this(null, messageId, content);
    }

    public SimpleStringMessage(UUID senderId, String content) {
        this(senderId, "default", content);
    }

    public SimpleStringClientMessage toClientMessage() {
        return new SimpleStringClientMessage(this.senderId, this.messageId, this.content);
    }

    public SimpleStringServerMessage toServerMessage() {
        return new SimpleStringServerMessage(this.senderId, this.messageId, this.content);
    }

    /** To Servers **/
    public class SimpleStringServerMessage extends SimpleStringMessage {
        /**  To Servers */
        public SimpleStringServerMessage(UUID senderId, String messageId, String content) {
            super(senderId, messageId, content);
        }
    }

    /** To Clients **/
    public class SimpleStringClientMessage extends SimpleStringMessage {
        /** To Clients */
        public SimpleStringClientMessage(UUID senderId, String messageId, String content) {
            super(senderId, messageId, content);
        }
    }

    /**
     * Clientbound AND serverbound event, avoid for integrated servers
     * @param p
     * @param messageId
     * @param content
     * @return
     */
    public static SimpleStringMessage createAndFire(Player p, String messageId, String content) {
        SimpleStringMessage message = (p==null) ? new SimpleStringMessage(messageId, content)
            : new SimpleStringMessage(p.getUUID(), messageId, content);

        if(GeneralConfig.getInstance().isIntegrated()) {
            EventRegistrar.getInstance().onSimpleMessage(p, message, message.messageId);
            ClientEventRegistrar.getInstance().onSimpleMessage(p, message, message.messageId);
            return message;
        }
        else if (GeneralConfig.getInstance().isServerSide()) {
            if(p==null) {
                String error = "SimpleStringMessage.createAndFire: Attempt to send message from server to undefined player. MsgId "+ messageId;
                LoggerBase.logError(null, "016001", error);
            }
            message = message.toClientMessage();
            HBUtil.NetworkUtil.serverSendToPlayer(p, message);
        } else {
            message = message.toServerMessage();
            HBUtil.NetworkUtil.clientSendToServer(message);
        }
        return message;
    }

    public static SimpleStringMessage createAndFire(String messageId, String content) {
        return createAndFire(null, messageId, content);
    }

    public static SimpleStringMessage createAndFireToAll(String messageId, String content) {
        List<Player> players = HBUtil.PlayerUtil.getAllSidedPlayers();
        SimpleStringMessage lastMessage = null;
        for(Player p : players) {
            lastMessage = createAndFire(p, messageId, content);
        }
        return lastMessage;
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
