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
            PlayerCallback.Attack.Before.EVENT.register(p(c).toPhase(), (player, target) -> {
                if (player.level().isClientSide()) return true; // server-only; client mirror in ClientBalmEventRegister
                PlayerAttackEvent event = new PlayerAttackEvent(player, target);
                c.accept(event);
                return !event.isCanceled();
            }));

        drainAndRegister(registrar.ON_BLOCK_BROKEN, "ON_BLOCK_BROKEN", c ->
            BlockCallback.Break.Before.EVENT.register(p(c).toPhase(), (level, pos, state, blockEntity, player) -> {
                if (level.isClientSide()) return true; // server-only; client mirror in ClientBalmEventRegister
                BreakBlockEvent event = new BreakBlockEvent(level, player, pos, state, blockEntity);
                c.accept(event);
                return !event.isCanceled();
            }));

        drainAndRegister(registrar.ON_PLAYER_CHANGED_DIMENSION, "ON_PLAYER_CHANGED_DIMENSION", c ->
            registry.onEvent(PlayerChangedDimensionEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_RESPAWN, "ON_PLAYER_RESPAWN", c ->
            registry.onEvent(PlayerRespawnEvent.class, c, p(c)));

        drainAndRegister(registrar.ON_PLAYER_DEATH, "ON_PLAYER_DEATH", c ->
            LivingEntityCallback.Death.Before.EVENT.register(p(c).toPhase(), (entity, damageSource) -> {
                if (entity.level().isClientSide()) return true; // server-only; client mirror in ClientBalmEventRegister
                LivingDeathEvent event = new LivingDeathEvent(entity, damageSource);
                c.accept(event);
                return !event.isCanceled();
            }));

        drainAndRegister(registrar.ON_PLAYER_DAMAGE, "ON_PLAYER_DAMAGE", c ->
            LivingEntityCallback.Damage.Before.EVENT.register(p(c).toPhase(), (entity, damageSource, damageAmount) -> {
                if (entity.level().isClientSide()) return damageAmount; // server-only; client mirror in ClientBalmEventRegister
                LivingDamageEvent event = new LivingDamageEvent(entity, damageSource, damageAmount);
                c.accept(event);
                return event.isCanceled() ? 0f : event.getDamageAmount();
            }));

        drainAndRegister(registrar.ON_PLAYER_FALL, "ON_PLAYER_FALL", c ->
            LivingEntityCallback.Fall.Before.EVENT.register(p(c).toPhase(), (entity, fallDamage) -> {
                if (entity.level().isClientSide()) return fallDamage; // server-only; client mirror in ClientBalmEventRegister
                LivingFallEvent event = new LivingFallEvent(entity, fallDamage);
                c.accept(event);
                if (event.isCanceled()) return 0f;
                return event.getFallDamageOverride() != null ? event.getFallDamageOverride() : fallDamage;
            }));

        drainAndRegister(registrar.ON_PLAYER_HEAL, "ON_PLAYER_HEAL", c ->
            LivingEntityCallback.Heal.Before.EVENT.register(p(c).toPhase(), (entity, healAmount) -> {
                if (entity.level().isClientSide()) return healAmount; // server-only; client mirror in ClientBalmEventRegister
                LivingHealEvent event = new LivingHealEvent(entity, healAmount);
                c.accept(event);
                return event.isCanceled() ? 0f : healAmount;
            }));

        drainAndRegister(registrar.ON_USE_BLOCK, "ON_USE_BLOCK", c ->
            BlockCallback.Use.EVENT.register(p(c).toPhase(), (player, level, hand, hitResult) -> {
                if (level.isClientSide()) return InteractionEventResult.DEFAULT; // server-only; client mirror in ClientBalmEventRegister
                UseBlockEvent event = new UseBlockEvent(player, level, hand, hitResult);
                c.accept(event);
                if (event.isCanceled() || event.getInteractionResult() != net.minecraft.world.InteractionResult.PASS) {
                    return () -> Optional.of(event.getInteractionResult());
                }
                return InteractionEventResult.DEFAULT;
            }));


        drainAndRegister(registrar.ON_DIG_SPEED_EVENT, "ON_DIG_SPEED_EVENT", c ->
            BlockCallback.DigSpeed.EVENT.register(p(c).toPhase(), (blockGetter, pos, state, player, digSpeed) -> {
                if (player.level().isClientSide()) return digSpeed; // server-only; client mirror in ClientBalmEventRegister
                DigSpeedEvent event = new DigSpeedEvent(player, state, digSpeed);
                c.accept(event);
                return event.getSpeedOverride() != null ? event.getSpeedOverride() : digSpeed;
            }));

        // Track wake up event registrations even though it's not a Balm event
        drainAndRegister(registrar.ON_WAKE_UP_ALL_PLAYERS, "ON_WAKE_UP_ALL_PLAYERS", c -> {});

        drainAndRegister(registrar.ON_TOSS_ITEM, "ON_TOSS_ITEM", c ->
            ItemCallback.Toss.Before.EVENT.register(p(c).toPhase(), (player, itemStack) -> {
                if (player.level().isClientSide()) return true; // server-only; client mirror in ClientBalmEventRegister
                TossItemEvent event = new TossItemEvent(player, itemStack);
                c.accept(event);
                return !event.isCanceled();
            }));
    }

    private static <T> void drainAndRegister(Set<Consumer<T>> set, String eventName, Consumer<Consumer<T>> balmRegistration)
    {
        Iterator<Consumer<T>> it = set.iterator();
        while (it.hasNext())
        {
            Consumer<T> c = it.next();
            String key = c.getClass().getName() + "::" + eventName;

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
        registry.onTickEvent(TickType.Server , TickPhase.Start, registrar::onServerTick);
        registry.onTickEvent(TickType.ServerLevel, TickPhase.Start, registrar::onServerLevelTick);

    }


}
