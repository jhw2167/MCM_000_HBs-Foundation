package com.holybuckets.foundation.console;

import com.holybuckets.foundation.CommonClass;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for messaging functionality across server and client sides
 */
public interface IMessager {


    public static IMessager getInstance() {
        return CommonClass.MESSAGER;
    }

    /**
     * Sends a message to the chat console for all players
     * @param message The message to send
     */
    void sendChat(String message);
    
    /**
     * Sends a message to the chat console for a specific player
     * @param player The target player
     * @param message The message to send
     */
    void sendChat(Player player, String message);
    
    /**
     * Sends heads up message to the player centered on bottom of their screen
     * @param player The target player
     * @param message The message to display
     */
    void sendBottomHeadsUp(Player player, String message);
    
    /**
     * Sends corner hint message to the player on the top right corner of their screen
     * @param player The target player
     * @param message The message to display
     */
    void sendCornerHint(Player player, String message);
    
    /**
     * Sends a bottom screen action hint message to a specific player
     * @param player The player to send the message to
     * @param message The message text to display
     */
    void sendBottomActionHint(Player player, String message);
    
    /**
     * Sends a bottom screen action hint message to all players
     * @param message The message text to display
     */
    void sendBottomActionHint(String message);
    
    /**
     * Sends a bottom screen error hint message to all players in red text
     * @param message The error message text to display
     */
    void bottomScreenErrorHint(String message);

    void bottomScreenErrorHint(Player player, String message);
}
