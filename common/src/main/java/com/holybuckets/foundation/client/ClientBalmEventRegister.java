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

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ClientBalmEventRegister {
    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static ClientEventRegistrar events;
    private static boolean notRegistered(Consumer<?> c) { return !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.get(func.hashCode()); }

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
