package com.holybuckets.foundation.event.balm.client;

import com.holybuckets.foundation.event.balm.BalmEvent;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class GuiDrawEvent extends BalmEvent {
    public enum Element {
        ALL,
        HEALTH,
        CHAT,
        DEBUG,
        BOSS_INFO,
        PLAYER_LIST
    }

    private final Window window;
    private final GuiGraphicsExtractor guiGraphics;
    private final Element element;

    public GuiDrawEvent(Window window, GuiGraphicsExtractor guiGraphics, Element element) {
        this.window = window;
        this.guiGraphics = guiGraphics;
        this.element = element;
    }

    public Window getWindow() {
        return window;
    }

    public GuiGraphicsExtractor getGuiGraphics() {
        return guiGraphics;
    }

    public Element getElement() {
        return element;
    }

    public static class Pre extends GuiDrawEvent {
        public Pre(Window window, GuiGraphicsExtractor guiGraphics, Element element) {
            super(window, guiGraphics, element);
        }
    }

    public static class Post extends GuiDrawEvent {
        public Post(Window window, GuiGraphicsExtractor guiGraphics, Element element) {
            super(window, guiGraphics, element);
        }
    }
}
