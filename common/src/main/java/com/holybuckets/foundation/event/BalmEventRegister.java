package com.holybuckets.foundation.event;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.command.BalmCommands;
import net.blay09.mods.balm.api.event.*;

import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Register all events in the Registrar with Balm Events
 */
public class BalmEventRegister {

    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static EventRegistrar events;
    private static boolean  notRegistered(Consumer<?> c) { return !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.get(func.hashCode()); }

    // Register all events in the Registrar with Balm Events
    public static void registerEvents()
    {
        BalmEvents registry = Balm.getEvents();
        events = EventRegistrar.getInstance();


        //** SERVER EVENTS **/

        events.ON_CHUNK_LOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkLoadingEvent.Load.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CHUNK_UNLOAD.stream().filter(BalmEventRegister::notRegistered).forEach( c -> {
            registry.onEvent( ChunkLoadingEvent.Unload.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });


        /** PLAYER EVENTS **/

        //Player login/logout events
        events.ON_PLAYER_LOGIN.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(PlayerLoginEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_PLAYER_LOGOUT.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(PlayerLogoutEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_PLAYER_ATTACK.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent( PlayerAttackEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_BLOCK_BROKEN.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent( BreakBlockEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        //support ON_PLAYER_CHANGE_DIMENSION
        events.ON_PLAYER_CHANGED_DIMENSION.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(PlayerChangedDimensionEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_PLAYER_RESPAWN.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(PlayerRespawnEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_PLAYER_DEATH.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(LivingDeathEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_USE_BLOCK.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(UseBlockEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_PLAYER_ATTACK_EVENT.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(PlayerAttackEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DIG_SPEED_EVENT.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(DigSpeedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        // Track wake up event registrations even though it's not a Balm event
        events.ON_WAKE_UP_ALL_PLAYERS.stream().filter(BalmEventRegister::notRegistered).forEach(c -> {
            registeredEvents.add(c.hashCode());
        });


    }


    public static void registerCommands() {
        BalmCommands commands = Balm.getCommands();
        commands.register(CommandRegistry::register);
    }

    static void registerPriorityEvents()
    {
        BalmEvents registry = Balm.getEvents();

        // Server lifecycle events with priority handling
        registry.onEvent(ServerStartingEvent.class, events::onBeforeServerStarted, EventPriority.Highest);
        registry.onEvent(ServerStartedEvent.class, events::onServerStarted, EventPriority.Highest);
        registry.onEvent(ServerStoppedEvent.class, events::onServerStopped, EventPriority.Lowest);

        // Level events with priority handling
        registry.onEvent(LevelLoadingEvent.Load.class, events::onLevelLoad, EventPriority.High);
        registry.onEvent(LevelLoadingEvent.Unload.class, events::onLevelUnload, EventPriority.Low);

        // Tick events
        if (registeredEvents.add(Objects.hash("onServerTick"))) {
            registry.onTickEvent(TickType.Server, TickPhase.End, events::onServerTick);
        }

        if (registeredEvents.add(Objects.hash("onServerLevelTick"))) {
            registry.onTickEvent(TickType.ServerLevel, TickPhase.Start, events::onServerLevelTick);
        }
    }


}
