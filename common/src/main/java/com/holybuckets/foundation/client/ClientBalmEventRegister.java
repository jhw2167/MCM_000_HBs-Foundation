package com.holybuckets.foundation.client;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.client.BlockHighlightDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ContainerScreenDrawEvent;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ClientBalmEventRegister {
    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static ClientEventRegistrar events;
    private static boolean notRegistered(Consumer<?> c) { return c!=null && !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.getOrDefault(func.hashCode(), EventPriority.Normal); }

    public static void registerEvents() {
        BalmEvents registry = Balm.getEvents();
        events = ClientEventRegistrar.getInstance();

        events.ON_CLIENT_STARTED_EVENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ClientStartedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONNECTED_TO_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ConnectedToServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DISCONNECTED_FROM_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(DisconnectedFromServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_BLOCK_HIGHLIGHT_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(BlockHighlightDrawEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ScreenDrawEvent.Pre.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ScreenDrawEvent.Post.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_BACKGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ContainerScreenDrawEvent.Background.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_FOREGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ContainerScreenDrawEvent.Foreground.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });
    }

    static void registerClientTickEvents() {
        BalmEvents registry = Balm.getEvents();

        if (registeredEvents.add(Objects.hash("onClientTick"))) {
            registry.onTickEvent(
                net.blay09.mods.balm.api.event.TickType.Client,
             TickPhase.End, events::onClientTick);
        }

        if (registeredEvents.add(Objects.hash("onClientLevelTick"))) {
            registry.onTickEvent(
                net.blay09.mods.balm.api.event.TickType.ClientLevel,
                 TickPhase.Start, events::onClientLevelTick);
        }
    }
}
