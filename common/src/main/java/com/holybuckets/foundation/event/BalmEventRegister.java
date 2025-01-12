package com.holybuckets.foundation.event;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.EventPriority;

import net.blay09.mods.balm.api.event.PlayerLoginEvent;
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

        //Server started event
        events.ON_SERVER_START.forEach( c -> {
            registry.onEvent( ServerStartedEvent.class, c, p(false));
        });

        //Server stopped event
        events.ON_SERVER_STOP.forEach( c -> {
            registry.onEvent( ServerStoppedEvent.class, c, p(false));
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
