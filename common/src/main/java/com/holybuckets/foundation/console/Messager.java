package com.holybuckets.foundation.console;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Description: Messaging class handles user friendly messaging via console or popup
 *
 * Singleton
 */
public class Messager {

    private static Messager INSTANCE;

    public static Messager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new Messager();
        return INSTANCE;
    }

    private Messager() {
    }

    /**
     * Sends a message to the chat console for all players
     * @param message
     */
    public void sendChat(String message) {

    }

    /**
    * Sends a message to the chat console for a specific player
    * @param player
    * @param message
    */
    public void sendChat(Player player, String message)
    {
        if (player == null || message == null || message.isEmpty()) return;
        if (player.level().isClientSide) return; // send only from server thread/side

        // Avoid chat-signing; send as system messages. Also avoid '\n' by sending lines.
        for (String line : message.split("\\R")) {
            if (!line.isEmpty()) {
                if(player instanceof ServerPlayer )
                    player.sendSystemMessage(Component.literal(line));
                else
                    player.displayClientMessage(Component.literal(line), false);
            }
        }
    }



}
