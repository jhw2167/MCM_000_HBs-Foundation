package com.holybuckets.foundation.event;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.command.BalmCommands;
import net.blay09.mods.balm.api.event.*;

import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Register all events in the Registrar with Balm Events
 */
public class BalmEventRegister {

    private static final Map<String, Consumer<?>> registeredEvents = new ConcurrentHashMap<>();
    private static EventRegistrar registrar;
    public static EventPriority p(Consumer<?> func) { return registrar.PRIORITIES.getOrDefault(func, EventPriority.Normal); }

    // Register all events in the Registrar with Balm Events
    public static void registerEvents()
    {
        BalmEvents registry = Balm.getEvents();
        registrar = EventRegistrar.getInstance();

        //** SERVER EVENTS **/

        drainAndRegister(registrar.ON_CHUNK_LOAD, "ON_CHUNK_LOAD", c ->
            registry.onEvent(ChunkLoadingEvent.Load.class, c, p(c)));

        drainAndRegister(registrar.ON_CHUNK_UNLOAD, "ON_CHUNK_UNLOAD", c ->
            registry.onEvent(ChunkLoadingEvent.Unload.class, c, p(c)));

        /** PLAYER EVENTS **/

        drainAndRegister(registrar.ON_PLAYER_LOGIN, "ON_PLAYER_LOGIN", c ->
            registry.onEvent(PlayerLoginEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_LOGOUT, "ON_PLAYER_LOGOUT", c ->
            registry.onEvent(PlayerLogoutEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_ATTACK, "ON_PLAYER_ATTACK", c ->
            registry.onEvent(PlayerAttackEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_BLOCK_BROKEN, "ON_BLOCK_BROKEN", c ->
            registry.onEvent(BreakBlockEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_CHANGED_DIMENSION, "ON_PLAYER_CHANGED_DIMENSION", c ->
            registry.onEvent(PlayerChangedDimensionEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_RESPAWN, "ON_PLAYER_RESPAWN", c ->
            registry.onEvent(PlayerRespawnEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_DEATH, "ON_PLAYER_DEATH", c ->
            registry.onEvent(LivingDeathEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_DAMAGE, "ON_PLAYER_DAMAGE", c ->
            registry.onEvent(LivingDamageEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_FALL, "ON_PLAYER_FALL", c ->
            registry.onEvent(LivingFallEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_HEAL, "ON_PLAYER_HEAL", c ->
            registry.onEvent(LivingHealEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_USE_BLOCK, "ON_USE_BLOCK", c ->
            registry.onEvent(UseBlockEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_ATTACK_EVENT, "ON_PLAYER_ATTACK_EVENT", c ->
            registry.onEvent(PlayerAttackEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_DIG_SPEED_EVENT, "ON_DIG_SPEED_EVENT", c ->
            registry.onEvent(DigSpeedEvent.class, c, p(c)));

        // Track wake up event registrations even though it's not a Balm event
        drainAndRegister(registrar.ON_WAKE_UP_ALL_PLAYERS, "ON_WAKE_UP_ALL_PLAYERS", c -> {});

        drainAndRegister(registrar.ON_TOSS_ITEM, "ON_TOSS_ITEM", c ->
            registry.onEvent(TossItemEvent.class, c, p(c)));
    }

    private static <T> void drainAndRegister(Set<Consumer<T>> set, String eventName, Consumer<Consumer<T>> balmRegistration)
    {
        Iterator<Consumer<T>> it = set.iterator();
        while (it.hasNext())
        {
            Consumer<T> c = it.next();
            String key = c.getClass().getName() + "::" + eventName;

            if(registeredEvents.containsKey(key)) {
                String i = "why do you happen";
            }

            if (!registeredEvents.containsKey(key))
            {
                balmRegistration.accept(c);
                registeredEvents.put(key, c);
            }
            it.remove();
        }
    }


    public static void registerCommands() {
        BalmCommands commands = Balm.getCommands();
        commands.register(CommandRegistry::register);
    }

    static void registerPriorityEvents(EventRegistrar registrar)
    {
        BalmEvents registry = Balm.getEvents();

        //Server Events
        registry.onEvent(ServerStartingEvent.class, registrar::onBeforeServerStarted, EventPriority.Highest);
        registry.onEvent(ServerStartedEvent.class, registrar::onServerStarted, EventPriority.Highest);
        registry.onEvent(ServerStoppedEvent.class, registrar::onServerStopped, EventPriority.Lowest);

        // Level events with priority handling
        registry.onEvent(LevelLoadingEvent.Load.class, registrar::onLevelLoad, EventPriority.High);
        registry.onEvent(LevelLoadingEvent.Unload.class, registrar::onLevelUnload, EventPriority.Low);

        // Tick events
        registry.onTickEvent(TickType.Server, TickPhase.End, registrar::onServerTick);
        registry.onTickEvent(TickType.ServerLevel, TickPhase.Start, registrar::onServerLevelTick);

    }


}
