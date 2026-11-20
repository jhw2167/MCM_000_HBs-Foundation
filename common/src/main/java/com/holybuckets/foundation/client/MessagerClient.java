package com.holybuckets.foundation.client;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.console.Messager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     */
    public void init(ClientEventRegistrar registrar) {
        // Subscribe to bottom screen action hint messages from server
        registrar.registerOnGuiDrawPost(this::onGuiDraw, EventPriority.Lowest);
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ACTION_HINT, (event) -> {
            bottomScreenHint(event.getContent(), 0xFFFFFF); // White text
        });
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ACTION_HINT_KEY, (event) -> {
            bottomScreenHint(Component.translatable(event.getContent()).getString(), 0xFFFFFF);
        });
        registrar.registerOnSimpleMessage(Messager.MSG_ID_BOTTOM_ERROR_HINT, (event) -> {
            bottomScreenHint(event.getContent(), 0xFF0000); // Red text
        });
    }
    
    private static boolean isServerCall(Player player) {
        if (player != null) return player instanceof ServerPlayer;
        return GeneralConfig.getInstance().isServerThread();
    }

    private static Messager server() {
        return Messager.getInstance();
    }

    // List to track active bottom screen messages
    private final Queue<BottomScreenMessage> bottomScreenMessages = new LinkedBlockingDeque<>();
    
    /**
     * Sends a message to the chat console for all players
     */
    @Override
    public void sendChat(String message) {
        if (isServerCall(null)) { server().sendChat(message); return; }
        Player player = Balm.getProxy().getClientPlayer();
        if (player != null) {
            sendChat(player, message);
        }
    }
    
    /**
     * Sends a message to the chat console for a specific player
     */
    @Override
    public void sendChat(Player player, String message) {
        if (isServerCall(player)) { server().sendChat(player, message); return; }
        if (player == null || message == null || message.isEmpty()) return;
        
        // On client side, display as client message
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), false);
    }
    
    /**
     * Sends heads up message to the player centered on bottom of their screen
     */
    @Override
    public void sendBottomHeadsUp(Player player, String message) {
        bottomScreenHint(message, 0xFFFFFF); // White text
    }
    
    /**
     * Sends corner hint message to the player on the top right corner of their screen
     */
    @Override
    public void sendCornerHint(Player player, String message) {
        // TODO: Implement corner hint display
    }

    /**
     * Sends a bottom screen action hint REFERENCING a translation key from the mod
     */
    @Override
    public void sendBottomActionHint(Player player, String modId, String translationKey) {
        if (isServerCall(player)) { server().sendBottomActionHint(player, modId, translationKey); return; }
        if(modId==null || modId.isEmpty()) {
            sendBottomActionHint(player, translationKey);
            return;
        }
        sendBottomActionHint(player, Component.translatable(translationKey).getString());
    }

    /**
     * Sends a bottom screen action hint message to a specific player
     */
    @Override
    public void sendBottomActionHint(Player player, String message) {
        if (isServerCall(player)) { server().sendBottomActionHint(player, message); return; }
        bottomScreenHint(message, 0xFFFFFF); // White text
    }
    
    /**
     * Sends a bottom screen action hint message to all players
     * @param message The message text to display
     */
    @Override
    public void sendBottomActionHint(String message) {
        if (isServerCall(null)) { server().sendBottomActionHint(message); return; }
        bottomScreenHint(message, 0xFFFFFF); // White text
    }

    /**
     * Sends a bottom screen error hint message to all players in red text
     */
    @Override
    public void bottomScreenErrorHint(String message) {
        if (isServerCall(null)) { server().bottomScreenErrorHint(message); return; }
        bottomScreenHint(message, 0xFF0000); // Red text
    }

    @Override
    public void bottomScreenErrorHint(Player player, String message) {
        if (isServerCall(player)) { server().bottomScreenErrorHint(player, message); return; }
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
            
            float alpha = message.getAlpha();
            int color = (int)(alpha * 255) << 24 | (message.textColor & 0xFFFFFF);
            int outlineColor = (int)(alpha * 255) << 24 | 0x000000;

            int y = screenHeight - 72 - yOffset;
            for (int i = message.lines.size() - 1; i >= 0; i--) {
                List<Object> line = message.lines.get(i);
                int x = (screenWidth - getWidth(font, line)) / 2;
                drawSegments(guiGraphics, font, line, x, y, color, outlineColor, alpha);
                int lineHeight = getLineHeight(line);
                y -= lineHeight;
                yOffset += lineHeight;
            }
        }
    }
    
    private static final Pattern ITEM_TOKEN = Pattern.compile("\\{I:(\\d{1,9})(:?)\\}");
    private static final int ICON_SIZE = 13;
    private static final float ICON_SCALE = ICON_SIZE / 16f;
    private static final int TEXT_LINE_HEIGHT = 12;
    private static final int MAX_LINE_CHARS = 100;

    private record ItemSegment(ItemStack stack, boolean showName) {}

    private static List<List<Object>> parseLines(String text) {
        List<List<Object>> lines = new ArrayList<>();
        for (String line : wrapLines(text)) lines.add(parseSegments(line));
        return lines;
    }

    private static List<String> wrapLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (line.length() > 0 && line.length() + 1 + word.length() > MAX_LINE_CHARS) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static int getLineHeight(List<Object> line) {
        for (Object segment : line) {
            if (segment instanceof ItemSegment) return ICON_SIZE + 2;
        }
        return TEXT_LINE_HEIGHT;
    }

    private static List<Object> parseSegments(String text) {
        List<Object> segments = new ArrayList<>();
        Matcher matcher = ITEM_TOKEN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            Item item = HBUtil.ItemUtil.getItem(Integer.parseInt(matcher.group(1)));
            if (item == null) continue;
            if (matcher.start() > last) segments.add(text.substring(last, matcher.start()));
            segments.add(new ItemSegment(new ItemStack(item), !matcher.group(2).isEmpty()));
            last = matcher.end();
        }
        if (last < text.length()) segments.add(text.substring(last));
        return segments;
    }

    private static String getItemLabel(ItemSegment item) {
        return item.showName() ? " " + item.stack().getHoverName().getString() : "";
    }

    private static int getWidth(Font font, List<Object> segments) {
        int width = 0;
        for (Object segment : segments) {
            if (segment instanceof ItemSegment item) width += ICON_SIZE + font.width(getItemLabel(item));
            else width += font.width((String) segment);
        }
        return width;
    }

    private void drawSegments(GuiGraphics guiGraphics, Font font, List<Object> segments, int x, int y, int color, int outlineColor, float alpha) {
        for (Object segment : segments) {
            String text;
            if (segment instanceof ItemSegment item) {
                if (alpha > 0.5f) drawIcon(guiGraphics, item.stack(), x, y);
                x += ICON_SIZE;
                text = getItemLabel(item);
            } else {
                text = (String) segment;
            }
            if (text.isEmpty()) continue;
            drawTextWithOutline(guiGraphics, font, text, x, y, color, outlineColor);
            x += font.width(text);
        }
    }

    private void drawIcon(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y - 2, 0);
        pose.scale(ICON_SCALE, ICON_SCALE, 1f);
        guiGraphics.renderItem(stack, 0, 0);
        pose.popPose();
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
        private final List<List<Object>> lines;
        private final int textColor;
        private final int durationMs;
        private final long startTime;
        private final int fadeInMs = 300;  // 300ms fade in
        private final int fadeOutMs = 500; // 500ms fade out
        
        /**
         * Constructor with custom text color
         */
        public BottomScreenMessage(String text, int textColor, int durationMs) {
            this.text = text;
            this.lines = parseLines(text);
            this.textColor = textColor;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * Constructor with default white text color
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
