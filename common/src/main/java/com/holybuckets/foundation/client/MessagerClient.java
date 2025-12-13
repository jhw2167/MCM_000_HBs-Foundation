package com.holybuckets.foundation.client;

import com.holybuckets.foundation.console.Messager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Description: Client-side messaging class for templated message types
 * 
 * Singleton
 */
public class MessagerClient {
    
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
        registrar.registerOnGuiDraw(this::onGuiDraw);
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ACTION_HINT, (event) -> {
            bottomScreenActionHint(event.getContent());
        });
    }
    
    // List to track active bottom screen messages
    private final List<BottomScreenMessage> bottomScreenMessages = new ArrayList<>();
    
    /**
     * Sends a hint message to the bottom center of the player's screen
     * @param message The message to display
     */
    public void bottomScreenActionHint(String message) {
        bottomScreenActionHint(message, 3000); // Default 3 second duration
    }
    
    /**
     * Sends a hint message to the bottom center of the player's screen
     * @param message The message to display
     * @param durationMs Duration in milliseconds to show the message
     */
    public void bottomScreenActionHint(String message, int durationMs) {
        if (message == null || message.isEmpty()) return;
        
        // Remove any existing messages with the same text to avoid duplicates
        bottomScreenMessages.removeIf(msg -> msg.text.equals(message));
        
        // Add new message
        bottomScreenMessages.add(new BottomScreenMessage(message, durationMs));
    }
    
    /**
     * Called during GUI rendering to draw active messages
     */
    private void onGuiDraw(GuiDrawEvent event) {
        if (bottomScreenMessages.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        //if (mc.screen != null) return; // Don't render when GUI is open
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();


        guiGraphics.fill(screenWidth / 2 - 50, screenHeight - 70,
            screenWidth / 2 + 50, screenHeight - 50,
            0x80FF0000); // Semi-transparent red box
        
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
            int color = (int)(alpha * 255) << 24 | 0xFFFFFF; // White text with alpha
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
        // Draw outline (8 directions)
        guiGraphics.drawString(font, text, x - 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, x, y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, x + 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, x - 1, y, outlineColor, false);
        guiGraphics.drawString(font, text, x + 1, y, outlineColor, false);
        guiGraphics.drawString(font, text, x - 1, y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, x, y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, x + 1, y + 1, outlineColor, false);
        
        // Draw main text
        guiGraphics.drawString(font, text, x, y, color, false);
    }
    
    /**
     * Internal class to track bottom screen messages
     */
    private static class BottomScreenMessage {
        private final String text;
        private final int durationMs;
        private final long startTime;
        private final int fadeInMs = 300;  // 300ms fade in
        private final int fadeOutMs = 500; // 500ms fade out
        
        public BottomScreenMessage(String text, int durationMs) {
            this.text = text;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
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
