package com.holybuckets.foundation.event;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.command.BalmCommands;
import net.blay09.mods.balm.api.event.*;

import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Register all events in the Registrar with Balm Events
 */
public class BalmEventRegister {

    private static final HashSet<Integer> registeredEvents = new HashSet<>();
    private static EventRegistrar events;
    private static boolean  notRegistered(Consumer<?> c) { return !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.get(func.hashCode()); }

    // Register all events in the Registrar with Balm Events
    public static void registerEvents()
    {
        BalmEvents registry = Balm.getEvents();
        events = EventRegistrar.getInstance();

        //** CLIENT EVENTS **/

        events.ON_CLIENT_STARTED_EVENT.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ClientStartedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONNECTED_TO_SERVER.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ConnectedToServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DISCONNECTED_FROM_SERVER.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( DisconnectedFromServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        //** SERVER EVENTS **/

        events.ON_BEFORE_SERVER_START.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent( ServerStartingEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SERVER_START.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ServerStartedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SERVER_STOP.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ServerStoppedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        /** LEVEL & CHUNK EVENTS **/

        events.ON_LEVEL_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( LevelLoadingEvent.Load.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_LEVEL_UNLOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( LevelLoadingEvent.Unload.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CHUNK_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkLoadingEvent.Load.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CHUNK_UNLOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkLoadingEvent.Unload.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });


        /** PLAYER EVENTS **/

        //Player login event
        events.ON_PLAYER_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( PlayerLoginEvent.class , c, p(c));
            registeredEvents.add(c.hashCode());
        });


    }


    public static void registerCommands() {
        BalmCommands commands = Balm.getCommands();
        commands.register(CommandRegistry::register);
    }



}
