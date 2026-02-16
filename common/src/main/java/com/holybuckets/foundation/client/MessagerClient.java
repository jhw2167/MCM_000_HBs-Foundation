package com.holybuckets.foundation.client;

import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.console.Messager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Description: Client-side messaging class for templated message types
 * 
 * Singleton
 */
public class MessagerClient implements IMessager {
    
    private static MessagerClient INSTANCE;
    
    public static MessagerClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessagerClient();
        }
        return INSTANCE;
    }
    
    private MessagerClient() {
    }
    
    /**
     * Initialize MessagerClient with ClientEventRegistrar to listen for server messages
     * @param registrar The ClientEventRegistrar instance
     */
    public void init(ClientEventRegistrar registrar) {
        // Subscribe to bottom screen action hint messages from server
        registrar.registerOnGuiDrawPost(this::onGuiDraw, EventPriority.Lowest);
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ACTION_HINT, (event) -> {
            bottomScreenHint(event.getContent(), 0xFFFFFF); // White text
        });
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ERROR_HINT, (event) -> {
            bottomScreenHint(event.getContent(), 0xFF0000); // Red text
        });
    }
    
    // List to track active bottom screen messages
    private final Queue<BottomScreenMessage> bottomScreenMessages = new LinkedBlockingDeque<>();
    
    /**
     * Sends a message to the chat console for all players
     * @param message The message to send
     */
    @Override
    public void sendChat(String message) {
        Player player = Balm.getProxy().getClientPlayer();
        if (player != null) {
            sendChat(player, message);
        }
    }
    
    /**
     * Sends a message to the chat console for a specific player
     * @param player The target player
     * @param message The message to send
     */
    @Override
    public void sendChat(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        
        // On client side, display as client message
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), false);
    }
    
    /**
     * Sends heads up message to the player centered on bottom of their screen
     * @param player The target player
     * @param message The message to display
     */
    @Override
    public void sendBottomHeadsUp(Player player, String message) {
        bottomScreenHint(message, 0xFFFFFF); // White text
    }
    
    /**
     * Sends corner hint message to the player on the top right corner of their screen
     * @param player The target player
     * @param message The message to display
     */
    @Override
    public void sendCornerHint(Player player, String message) {
        // TODO: Implement corner hint display
    }
    
    /**
     * Sends a bottom screen action hint message to a specific player
     * @param player The player to send the message to
     * @param message The message text to display
     */
    @Override
    public void sendBottomActionHint(Player player, String message) {
        bottomScreenHint(message, 0xFFFFFF); // White text
    }
    
    /**
     * Sends a bottom screen action hint message to all players
     * @param message The message text to display
     */
    @Override
    public void sendBottomActionHint(String message) {
        bottomScreenHint(message, 0xFFFFFF); // White text
    }

    /**
     * Sends a bottom screen error hint message to all players in red text
     * @param message The error message text to display
     */
    @Override
    public void bottomScreenErrorHint(String message) {
        bottomScreenHint(message, 0xFF0000); // Red text
    }

    @Override
    public void bottomScreenErrorHint(Player player, String message) {
        bottomScreenHint(message, 0xFF0000); // Red text
    }

    /**
     * General method to send a hint message to the bottom center of the player's screen
     * @param message The message to display
     * @param textColor The color of the text (RGB format)
     */
    private void bottomScreenHint(String message, int textColor) {
        bottomScreenHint(message, textColor, 5000); // Default 5 second duration
    }
    
    /**
     * General method to send a hint message to the bottom center of the player's screen
     * @param message The message to display
     * @param textColor The color of the text (RGB format)
     * @param durationMs Duration in milliseconds to show the message
     */
    private void bottomScreenHint(String message, int textColor, int durationMs) {
        if (message == null || message.isEmpty()) return;
        
        // Remove any existing messages with the same text to avoid duplicates
        bottomScreenMessages.removeIf(msg -> msg.text.equals(message));
        
        // Add new message
        bottomScreenMessages.add(new BottomScreenMessage(message, textColor, durationMs));
    }
    
    /**
     * Called during GUI rendering to draw active messages
     */
    private void onGuiDraw(GuiDrawEvent.Post event) {
        if (bottomScreenMessages.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        //if (mc.screen != null) return; // Don't render when GUI is open
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        
        // Update and render bottom screen messages
        Iterator<BottomScreenMessage> iterator = bottomScreenMessages.iterator();
        int yOffset = 0;
        
        while (iterator.hasNext()) {
            BottomScreenMessage message = iterator.next();
            message.update();
            
            if (message.isExpired()) {
                iterator.remove();
                continue;
            }
            
            // Calculate position (bottom center, with offset for multiple messages)
            int textWidth = font.width(message.text);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 60 - yOffset; // 60 pixels from bottom, stacked upward
            
            // Calculate alpha based on fade in/out
            float alpha = message.getAlpha();
            int color = (int)(alpha * 255) << 24 | (message.textColor & 0xFFFFFF); // Apply alpha to text color
            int outlineColor = (int)(alpha * 255) << 24 | 0x000000; // Black outline with alpha
            
            // Draw text with outline effect
            drawTextWithOutline(guiGraphics, font, message.text, x, y, color, outlineColor);
            
            yOffset += 12; // Stack messages vertically
        }
    }
    
    /**
     * Draws text with a black outline for better visibility
     */
    private void drawTextWithOutline(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, int outlineColor) {
        // Draw outline (4 cardinal directions only - thinner)
        guiGraphics.drawString(font, text, x - 1, y, outlineColor, false);     // Left
        guiGraphics.drawString(font, text, x + 1, y, outlineColor, false);     // Right
        guiGraphics.drawString(font, text, x, y - 1, outlineColor, false);     // Top
        guiGraphics.drawString(font, text, x, y + 1, outlineColor, false);     // Bottom

        // Draw main text
        guiGraphics.drawString(font, text, x, y, color, false);
    }
    
    /**
     * Internal class to track bottom screen messages
     */
    private static class BottomScreenMessage {
        private final String text;
        private final int textColor;
        private final int durationMs;
        private final long startTime;
        private final int fadeInMs = 300;  // 300ms fade in
        private final int fadeOutMs = 500; // 500ms fade out
        
        /**
         * Constructor with custom text color
         * @param text The message text
         * @param textColor The color of the text (RGB format)
         * @param durationMs Duration in milliseconds
         */
        public BottomScreenMessage(String text, int textColor, int durationMs) {
            this.text = text;
            this.textColor = textColor;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * Constructor with default white text color
         * @param text The message text
         * @param durationMs Duration in milliseconds
         */
        public BottomScreenMessage(String text, int durationMs) {
            this(text, 0xFFFFFF, durationMs); // Default to white
        }
        
        public void update() {
            // Nothing to update per tick currently
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > durationMs;
        }
        
        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (elapsed < fadeInMs) {
                // Fade in
                return Mth.clamp((float) elapsed / fadeInMs, 0.0f, 1.0f);
            } else if (elapsed > durationMs - fadeOutMs) {
                // Fade out
                long fadeOutElapsed = elapsed - (durationMs - fadeOutMs);
                return Mth.clamp(1.0f - (float) fadeOutElapsed / fadeOutMs, 0.0f, 1.0f);
            } else {
                // Fully visible
                return 1.0f;
            }
        }
    }
}
