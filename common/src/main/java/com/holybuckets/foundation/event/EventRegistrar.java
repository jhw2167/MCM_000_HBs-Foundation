package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.*;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.holybuckets.foundation.util.MixinManager;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.BreakBlockEvent;
import net.blay09.mods.balm.api.event.PlayerAttackEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class EventRegistrar {
    public static final String CLASS_ID = "010";

    /**
     * World Data
     **/
    private static EventRegistrar instance;
    final Map<Integer, EventPriority> PRIORITIES = new HashMap<>();

    final Set<Consumer<PlayerLoginEvent>> ON_PLAYER_LOGIN = new ConcurrentSet<>();
    final Set<Consumer<PlayerLogoutEvent>> ON_PLAYER_LOGOUT = new ConcurrentSet<>();
    final Set<Consumer<LevelLoadingEvent.Load>> ON_LEVEL_LOAD = new ConcurrentSet<>();
    final Set<Consumer<LevelLoadingEvent.Unload>> ON_LEVEL_UNLOAD = new ConcurrentSet<>();


    final Set<Consumer<ChunkLoadingEvent.Load>> ON_CHUNK_LOAD = new ConcurrentSet<>();
    final Set<Consumer<ChunkLoadingEvent.Unload>> ON_CHUNK_UNLOAD = new ConcurrentSet<>();

    //final Deque<Consumer<ModLifecycleEvent>> ON_MOD_LIFECYCLE = new ArrayDeque<>();
    //Will have to divide up into different lifecycles

    //final Deque<Consumer<RegisterEvent>> ON_REGISTER = new ArrayDeque<>();
    //Dont see it, I think this is for registering commands

    //final Deque<Consumer<ModConfigEvent>> ON_MOD_CONFIG = new ArrayDeque<>();
    //Dont see it, is probably different for forge and fabric, Balm abstracts away all configuration

    final Set<Consumer<ServerStartingEvent>> ON_BEFORE_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStartedEvent>> ON_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStoppedEvent>> ON_SERVER_STOP = new ConcurrentSet<>();

    final Map<TickScheme, Consumer<?>> SERVER_TICK_EVENTS = new ConcurrentHashMap<>();
    final Set<Consumer<DatastoreSaveEvent>> ON_DATA_SAVE = new ConcurrentSet<>();
    final Set<Consumer<PlayerAttackEvent>> ON_PLAYER_ATTACK = new ConcurrentSet<>();
    final Set<Consumer<BreakBlockEvent>> ON_BLOCK_BROKEN = new ConcurrentSet<>();
    final Set<Consumer<PlayerChangedDimensionEvent>> ON_PLAYER_CHANGED_DIMENSION = new ConcurrentSet<>();
    final Set<Consumer<PlayerRespawnEvent>> ON_PLAYER_RESPAWN = new ConcurrentSet<>();
    final Set<Consumer<LivingDeathEvent>> ON_PLAYER_DEATH = new ConcurrentSet<>();
    final Set<Consumer<UseBlockEvent>> ON_USE_BLOCK = new ConcurrentSet<>();
    final Set<Consumer<PlayerAttackEvent>> ON_PLAYER_ATTACK_EVENT = new ConcurrentSet<>();
    final Set<Consumer<DigSpeedEvent>> ON_DIG_SPEED_EVENT = new ConcurrentSet<>();
    final Set<Consumer<ClientInputEvent>> ON_CLIENT_INPUT = new ConcurrentSet<>();

    /**
     * Constructor
     **/
    private EventRegistrar() {
        super();
        LoggerBase.logInit(null, "010000", this.getClass().getName());

        instance = this;
    }

    public static EventRegistrar getInstance() {
        return instance;
    }

    public static void init() {
        instance = new EventRegistrar();
        instance.registerOnBeforeServerStarted(instance::beforeServerStarted);
    }

    private void beforeServerStarted(ServerStartingEvent event) {
        BalmEventRegister.registerOnTickEvents();
    }


    //Create public methods for pushing functions onto each function event
    private <T> void generalRegister(Consumer<T> function, Set<Consumer<T>> set, EventPriority priority) {
        set.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    private void generalTickEventRegister(Consumer<?> function, Map<TickScheme, Consumer<?>> map, TickType type, EventPriority priority) {
        TickScheme scheme = new TickScheme(function, type);
        map.put(scheme, function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function) {
        registerOnPlayerLogin(function, EventPriority.Normal);
    }

    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGIN, priority);
    }

    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function) {
        registerOnPlayerLogout(function, EventPriority.Normal);
    }

    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGOUT, priority);
    }


    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function) {
        registerOnLevelLoad(function, EventPriority.Normal);
    }

    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_LOAD, priority);
    }


    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function) {
        registerOnLevelUnload(function, EventPriority.Normal);
    }

    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_UNLOAD, priority);
    }


    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function) {
        registerOnChunkLoad(function, EventPriority.Normal);
    }

    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_CHUNK_LOAD, priority);
    }

    public void registerOnChunkUnload(Consumer<ChunkLoadingEvent.Unload> function) {
        registerOnChunkUnload(function, EventPriority.Normal);
    }

    public void registerOnChunkUnload(Consumer<ChunkLoadingEvent.Unload> function, EventPriority priority) {
        generalRegister(function, ON_CHUNK_UNLOAD, priority);
    }

    /*
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function) { registerOnModLifecycle(function, false); }
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function, EventPriority priority) {
        generalRegister(function, ON_MOD_LIFECYCLE, priority);
    }

    public void registerOnRegister(Consumer<RegisterEvent> function) { registerOnRegister(function, false); }
    public void registerOnRegister(Consumer<RegisterEvent> function, EventPriority priority) {
        generalRegister(function, ON_REGISTER, priority);
    }

    public void registerOnModConfig(Consumer<ModConfigEvent> function) { registerOnModConfig(function, false); }
    public void registerOnModConfig(Consumer<ModConfigEvent> function, EventPriority priority) {
        generalRegister(function, ON_MOD_CONFIG, priority);
    }
    */


    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function) {
        registerOnBeforeServerStarted(function, EventPriority.Normal);
    }

    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function, EventPriority priority) {
        generalRegister(function, ON_BEFORE_SERVER_START, priority);
    }


    public void registerOnServerStarted(Consumer<ServerStartedEvent> function) {
        registerOnServerStarted(function, EventPriority.Normal);
    }

    public void registerOnServerStarted(Consumer<ServerStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_START, priority);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function) {
        registerOnServerStopped(function, EventPriority.Normal);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_STOP, priority);
    }



    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function) {
        registerOnDataSave(function, EventPriority.Normal);
    }

    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function, EventPriority priority) {
        generalRegister(function, ON_DATA_SAVE, priority);
    }


    //** TICK EVENTS

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function) {
        registerOnServerTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, SERVER_TICK_EVENTS, type, priority);
    }




    //** PLAYER EVENTS

    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function) {
        registerOnPlayerAttack(function, EventPriority.Normal);
    }

    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_ATTACK, priority);
    }

    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function) {
        registerOnBreakBlock(function, EventPriority.Normal);
    }

    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function, EventPriority priority) {
        generalRegister(function, ON_BLOCK_BROKEN, priority);
    }

    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function) {
        registerOnPlayerChangedDimension(function, EventPriority.Normal);
    }

    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_CHANGED_DIMENSION, priority);
    }

    public void registerOnPlayerRespawn(Consumer<PlayerRespawnEvent> function) {
        registerOnPlayerRespawn(function, EventPriority.Normal);
    }

    public void registerOnPlayerRespawn(Consumer<PlayerRespawnEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_RESPAWN, priority);
    }

    public void registerOnPlayerDeath(Consumer<LivingDeathEvent> function) {
        registerOnPlayerDeath(function, EventPriority.Normal);
    }

    public void registerOnPlayerDeath(Consumer<LivingDeathEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_DEATH, priority);
    }

    public void registerOnUseBlock(Consumer<UseBlockEvent> function) {
        registerOnUseBlock(function, EventPriority.Normal);
    }

    public void registerOnUseBlock(Consumer<UseBlockEvent> function, EventPriority priority) {
        generalRegister(function, ON_USE_BLOCK, priority);
    }

    public void registerOnPlayerAttackEvent(Consumer<PlayerAttackEvent> function) {
        registerOnPlayerAttackEvent(function, EventPriority.Normal);
    }

    public void registerOnPlayerAttackEvent(Consumer<PlayerAttackEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_ATTACK_EVENT, priority);
    }

    public void registerOnDigSpeedEvent(Consumer<DigSpeedEvent> function) {
        registerOnDigSpeedEvent(function, EventPriority.Normal);
    }

    public void registerOnDigSpeedEvent(Consumer<DigSpeedEvent> function, EventPriority priority) {
        generalRegister(function, ON_DIG_SPEED_EVENT, priority);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function) {
        registerOnClientInput(function, EventPriority.Normal);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_INPUT, priority);
    }

    /**
     * Custom Events
     **/

    public void dataSaveEvent() {
        DatastoreSaveEvent event = DatastoreSaveEvent.create();
        for (Consumer<DatastoreSaveEvent> saver : ON_DATA_SAVE) {
            saver.accept(event);
        }
        event.getDataStore().save();
    }

    public void onServerTick(MinecraftServer s) {
        long totalTicks = GeneralConfig.getInstance().getTotalTickCount();
        ServerTickEvent event = new ServerTickEvent(totalTicks);
        //LoggerBase.logDebug(null, "010001", "Server tick event: " + totalTicks);
        SERVER_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (totalTicks % scheme.getFrequency() == scheme.offset) {
                tryEvent((Consumer<ServerTickEvent>) consumer, event);
            }
        });
    }

    public void onServerLevelTick(Level level) {
        if( level == null ) return;
        ManagedChunkEvents.onWorldTickStart(level);
    }


    public void onClientInput(ClientInputMessage message) {
        GeneralConfig config = GeneralConfig.getInstance();
        Player p = config.getServer().getPlayerList().getPlayer(message.playerId);
        ClientInputEvent event = new ClientInputEvent(p, message);
        ON_CLIENT_INPUT.forEach(consumer -> tryEvent(consumer, event));
    }

        private <T> void tryEvent(Consumer<T> consumer, T event) {
            String id = consumer.toString() + "::" + event.getClass().getName();
            if( MixinManager.isEnabled(consumer.toString())) {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    MixinManager.recordError(id, e);
                }
            }
        }


    /**
     * ###############
     **/

    private class TickScheme {
        int offset;
        TickType frequency;

        <T> TickScheme(Consumer<T> func, TickType frequency) {
            this.frequency = frequency;
            this.offset = (func.hashCode() % getFrequency());
        }

        int getFrequency() {
            switch (frequency) {
                case ON_SINGLE_TICK:
                    return 1;
                case ON_20_TICKS:
                    return 20;
                case ON_120_TICKS:
                    return 120;
                case ON_1200_TICKS:
                    return 1200;
                case DAILY_TICK:
                    return 24000; // 1 day in ticks
                default:
                    return 1;
            }

        }

    }
}
//END CLASS
