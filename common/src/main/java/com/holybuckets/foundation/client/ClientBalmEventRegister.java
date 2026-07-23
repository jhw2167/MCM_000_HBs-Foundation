package com.holybuckets.foundation.client;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.client.ClientStartedEvent;
import com.holybuckets.foundation.event.balm.client.ConnectedToServerEvent;
import com.holybuckets.foundation.event.balm.client.DisconnectedFromServerEvent;
import com.holybuckets.foundation.event.balm.client.BlockHighlightDrawEvent;
import com.holybuckets.foundation.event.balm.client.GuiDrawEvent;
import com.holybuckets.foundation.event.balm.client.screen.ScreenDrawEvent;
import com.holybuckets.foundation.event.balm.client.screen.ContainerScreenDrawEvent;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.event.callback.RenderCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ClientBalmEventRegister {
    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static ClientEventRegistrar events;
    private static boolean notRegistered(Consumer<?> c) { return c!=null && !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.getOrDefault(func.hashCode(), EventPriority.Normal); }

    public static void registerEvents() {
        events = ClientEventRegistrar.getInstance();

        events.ON_CLIENT_STARTED_EVENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.Started.EVENT.register(p(c).toPhase(), client ->
                c.accept(new ClientStartedEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONNECTED_TO_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.ConnectedToServer.EVENT.register(p(c).toPhase(), client ->
                c.accept(new ConnectedToServerEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DISCONNECTED_FROM_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.DisconnectedFromServer.EVENT.register(p(c).toPhase(), client ->
                c.accept(new DisconnectedFromServerEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_BLOCK_HIGHLIGHT_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.BlockHighlight.EVENT.register(p(c).toPhase(), (hitResult, poseStack, multiBufferSource, camera, color, lineWidth) -> {
                BlockHighlightDrawEvent event = new BlockHighlightDrawEvent(hitResult, poseStack, multiBufferSource, camera);
                c.accept(event);
                return !event.isCanceled();
            });
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.BEFORE.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ScreenDrawEvent.Pre(screen, guiGraphics, mouseX, mouseY, delta)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ScreenDrawEvent.Post(screen, guiGraphics, mouseX, mouseY, delta)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_BACKGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER_BACKGROUND.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ContainerScreenDrawEvent.Background(screen, guiGraphics, mouseX, mouseY)));
            registeredEvents.add(c.hashCode());
        });

        // Balm 26.1 has no dedicated foreground (renderLabels) hook; Render.AFTER is the closest equivalent
        events.ON_CONTAINER_SCREEN_DRAW_FOREGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ContainerScreenDrawEvent.Foreground(screen, guiGraphics, mouseX, mouseY)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.AFTER.register(p(c).toPhase(), (guiGraphics, window) ->
                c.accept(new GuiDrawEvent.Post(window, guiGraphics, GuiDrawEvent.Element.ALL)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.BEFORE.register(p(c).toPhase(), (guiGraphics, window) -> {
                GuiDrawEvent.Pre event = new GuiDrawEvent.Pre(window, guiGraphics, GuiDrawEvent.Element.ALL);
                c.accept(event);
                return !event.isCanceled();
            });
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.AFTER.register(p(c).toPhase(), (guiGraphics, window) ->
                c.accept(new GuiDrawEvent.Post(window, guiGraphics, GuiDrawEvent.Element.ALL)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_ELEMENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.Health.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.HEALTH));
            RenderCallback.Gui.Chat.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.CHAT));
            RenderCallback.Gui.Debug.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.DEBUG));
            RenderCallback.Gui.BossInfo.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.BOSS_INFO));
            RenderCallback.Gui.PlayerList.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.PLAYER_LIST));
            registeredEvents.add(c.hashCode());
        });

    }

    static void registerClientTickEvents() {
        if (registeredEvents.add(Objects.hash("onClientTick"))) {
            ClientTickCallback.AFTER.register(events::onClientTick);
        }

        if (registeredEvents.add(Objects.hash("onClientLevelTick"))) {
            ClientTickCallback.ClientLevelTick.AFTER.register(events::onClientLevelTick);
        }
    }
}
