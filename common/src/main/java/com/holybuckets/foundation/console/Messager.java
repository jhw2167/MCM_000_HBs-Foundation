package com.holybuckets.foundation.console;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
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
public class Messager implements IMessager {

    public static final String MSG_ID_BOTTOM_ACTION_HINT = "bottom_screen_action_hint";
    public static final String MSG_ID_BOTTOM_ERROR_HINT = "bottom_screen_error_hint";

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
    @Override
    public void sendChat(String message) {
        for(Player p : HBUtil.PlayerUtil.getAllPlayers()) {
            sendChat(p, message);
        }
    }

    /**
    * Sends a message to the chat console for a specific player
    * @param player
    * @param message
    */
    @Override
    public void sendChat(Player player, String message)
    {
        if (player == null || message == null || message.isEmpty()) return;

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

    /**
     * Sends heads up message to the player centered on bottom of their screen
     * @param p
     * @param message
     */
    @Override
    public void sendBottomHeadsUp(Player p, String message) {
    }


    /**
     * Sends corner hint message to the player on the top right corner of their screen
     * @param p
     * @param message
     */
    @Override
    public void sendCornerHint(Player p, String message) {
    }

    /**
     * Sends a bottom screen action hint message from server to client
     * @param player The player to send the message to
     * @param message The message text to display
     */
    @Override
    public void sendBottomActionHint(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        
        SimpleStringMessage.createAndFire(player, MSG_ID_BOTTOM_ACTION_HINT, message);
    }

    /**
     * Sends a bottom screen action hint message from server to all players
     * @param message The message text to display
     */
    @Override
    public void sendBottomActionHint(String message) {
        for(Player p : HBUtil.PlayerUtil.getAllPlayers()) {
            sendBottomActionHint(p, message);
        }
    }

    /**
     * Sends a bottom screen error hint message from server to all players in red text
     * @param message The error message text to display
     */
    @Override
    public void bottomScreenErrorHint(String message) {
        for(Player p : HBUtil.PlayerUtil.getAllPlayers()) {
            SimpleStringMessage.createAndFire(p, MSG_ID_BOTTOM_ERROR_HINT, message);
        }
    }
}
