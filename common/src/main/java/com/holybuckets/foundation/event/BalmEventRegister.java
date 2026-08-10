package com.holybuckets.foundation.event;

import com.holybuckets.foundation.event.balm.*;
import com.holybuckets.foundation.event.balm.server.ServerStartedEvent;
import com.holybuckets.foundation.event.balm.server.ServerStartingEvent;
import com.holybuckets.foundation.event.balm.server.ServerStoppedEvent;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.commands.BalmCommands;
import net.blay09.mods.balm.platform.event.callback.BlockCallback;
import net.blay09.mods.balm.platform.event.callback.InteractionEventResult;
import net.blay09.mods.balm.platform.event.callback.ItemCallback;
import net.blay09.mods.balm.platform.event.callback.LevelCallback;
import net.blay09.mods.balm.platform.event.callback.LivingEntityCallback;
import net.blay09.mods.balm.platform.event.callback.PlayerCallback;
import net.blay09.mods.balm.platform.event.callback.ServerLifecycleCallback;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.blay09.mods.balm.platform.event.callback.ServerTickCallback;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
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
        registrar = EventRegistrar.getInstance();

        //** SERVER EVENTS **/

        drainAndRegister(registrar.ON_CHUNK_LOAD, "ON_CHUNK_LOAD", c ->
            LevelCallback.Chunk.LOAD.register(p(c).toPhase(), (level, chunk, chunkPos) ->
                c.accept(new ChunkLoadingEvent.Load(level, chunk, chunkPos))));

        drainAndRegister(registrar.ON_CHUNK_UNLOAD, "ON_CHUNK_UNLOAD", c ->
            LevelCallback.Chunk.UNLOAD.register(p(c).toPhase(), (level, chunk, chunkPos) ->
                c.accept(new ChunkLoadingEvent.Unload(level, chunk, chunkPos))));

        /** PLAYER EVENTS **/

        drainAndRegister(registrar.ON_PLAYER_LOGIN, "ON_PLAYER_LOGIN", c ->
            ServerPlayerCallback.Join.EVENT.register(p(c).toPhase(), player ->
                c.accept(new PlayerLoginEvent(player))));

        drainAndRegister(registrar.ON_PLAYER_LOGOUT, "ON_PLAYER_LOGOUT", c ->
            ServerPlayerCallback.Leave.EVENT.register(p(c).toPhase(), player ->
                c.accept(new PlayerLogoutEvent(player))));

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
            ServerPlayerCallback.DimensionChange.EVENT.register(p(c).toPhase(), (player, from, to) ->
                c.accept(new PlayerChangedDimensionEvent(player, from, to))));

        drainAndRegister(registrar.ON_PLAYER_RESPAWN, "ON_PLAYER_RESPAWN", c ->
            ServerPlayerCallback.Respawn.EVENT.register(p(c).toPhase(), (oldPlayer, newPlayer) ->
                c.accept(new PlayerRespawnEvent(oldPlayer, newPlayer))));

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

        drainAndRegister(registrar.ON_PLAYER_ATTACK_EVENT, "ON_PLAYER_ATTACK_EVENT", c ->
            PlayerCallback.Attack.Before.EVENT.register(p(c).toPhase(), (player, target) -> {
                if (player.level().isClientSide()) return true; // server-only; client mirror in ClientBalmEventRegister
                PlayerAttackEvent event = new PlayerAttackEvent(player, target);
                c.accept(event);
                return !event.isCanceled();
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
        BalmCommands commands = Balm.commands();
        commands.register(CommandRegistry::register);
    }

    static void registerPriorityEvents(EventRegistrar registrar)
    {
        //Server Events
        ServerLifecycleCallback.Starting.EVENT.register(EventPriority.Highest.toPhase(), server ->
            registrar.onBeforeServerStarted(new ServerStartingEvent(server)));
        ServerLifecycleCallback.Started.EVENT.register(EventPriority.Highest.toPhase(), server ->
            registrar.onServerStarted(new ServerStartedEvent(server)));
        ServerLifecycleCallback.Stopped.EVENT.register(EventPriority.Lowest.toPhase(), server ->
            registrar.onServerStopped(new ServerStoppedEvent(server)));

        // Level events with priority handling
        LevelCallback.LOAD.register(EventPriority.High.toPhase(), level ->
            registrar.onLevelLoad(new LevelLoadingEvent.Load(level)));
        LevelCallback.UNLOAD.register(EventPriority.Low.toPhase(), level ->
            registrar.onLevelUnload(new LevelLoadingEvent.Unload(level)));

        // Tick events
        ServerTickCallback.BEFORE.register(registrar::onServerTick);
        ServerTickCallback.ServerLevelTick.BEFORE.register(registrar::onServerLevelTick);
    }


}
