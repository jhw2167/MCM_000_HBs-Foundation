package com.holybuckets.foundation.event;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.*;

import net.blay09.mods.balm.api.event.server.ServerBeforeStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Register all events in the Registrar with Balm Events
 */
public class BalmEventRegister {

    private static final HashSet<Integer> registeredEvents = new HashSet<>();
    private static boolean  notRegistered(Consumer<?> c) { return !registeredEvents.contains(c.hashCode()); }


    // Register all events in the Registrar with Balm Events
    public static void registerEvents()
    {
        BalmEvents registry = Balm.getEvents();
        EventRegistrar events = EventRegistrar.getInstance();

        /** SERVER EVENTS **/

        events.ON_BEFORE_SERVER_START.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent( ServerBeforeStartingEvent.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SERVER_START.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ServerStartedEvent.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SERVER_STOP.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ServerStoppedEvent.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        /** LEVEL & CHUNK EVENTS **/

        events.ON_LEVEL_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( LevelEvent.Load.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        events.ON_LEVEL_UNLOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( LevelEvent.Unload.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CHUNK_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkEvent.Load.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CHUNK_UNLOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkEvent.Unload.class, c, p(false));
            registeredEvents.add(c.hashCode());
        });


        /** PLAYER EVENTS **/

        //Player login event
        events.ON_PLAYER_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( PlayerLoginEvent.class , c, p(false));
            registeredEvents.add(c.hashCode());
        });


    }

    public static EventPriority p(boolean isHigh) {
        return isHigh ? EventPriority.High  : EventPriority.Normal;
    }



}
