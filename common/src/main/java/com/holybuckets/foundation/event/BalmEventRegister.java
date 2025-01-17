package com.holybuckets.foundation.event;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.*;

import net.blay09.mods.balm.api.event.server.ServerBeforeStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

/**
 * Register all events in the Registrar with Balm Events
 */
public class BalmEventRegister {

    // Register all events in the Registrar with Balm Events
    public static void registerEvents()
    {
        BalmEvents registry = Balm.getEvents();
        EventRegistrar events = EventRegistrar.getInstance();

        /** SERVER EVENTS **/

        events.ON_BEFORE_SERVER_START.forEach( c -> {
            registry.onEvent( ServerBeforeStartingEvent.class, c, p(false));
        });

        events.ON_SERVER_START.forEach( c -> {
            registry.onEvent( ServerStartedEvent.class, c, p(false));
        });

        events.ON_SERVER_STOP.forEach( c -> {
            registry.onEvent( ServerStoppedEvent.class, c, p(false));
        });

        /** LEVEL & CHUNK EVENTS **/

        events.ON_LEVEL_LOAD.forEach( c -> {
            registry.onEvent( LevelEvent.Load.class, c, p(false));
        });

        events.ON_LEVEL_UNLOAD.forEach( c -> {
            registry.onEvent( LevelEvent.Unload.class, c, p(false));
        });

        events.ON_CHUNK_LOAD.forEach( c -> {
            registry.onEvent( ChunkEvent.Load.class, c, p(false));
        });

        events.ON_CHUNK_UNLOAD.forEach( c -> {
            registry.onEvent( ChunkEvent.Unload.class, c, p(false));
        });


        /** PLAYER EVENTS **/

        //Player login event
        events.ON_PLAYER_LOAD.forEach( c -> {
            registry.onEvent( PlayerLoginEvent.class , c, p(false));
        });


    }

    public static EventPriority p(boolean isHigh) {
        return isHigh ? EventPriority.High  : EventPriority.Normal;
    }



}
