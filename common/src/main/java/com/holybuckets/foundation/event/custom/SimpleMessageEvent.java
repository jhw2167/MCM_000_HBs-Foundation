package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Custom event for handling simple string messages with messageId routing
 */
public class SimpleMessageEvent {

    @Nullable
    private final Player player;
    private final SimpleStringMessage message;
    private final String messageId;
    
    public SimpleMessageEvent(Player player, SimpleStringMessage message, String messageId) {
        this.player = player;
        this.message = message;
        this.messageId = messageId;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public SimpleStringMessage getMessage() {
        return message;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getContent() {
        return message.content;
    }
}
