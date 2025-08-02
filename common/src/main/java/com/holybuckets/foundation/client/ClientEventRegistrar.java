package com.holybuckets.foundation.client;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.event.custom.*;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.holybuckets.foundation.util.MixinManager;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class ClientEventRegistrar {
    public static final String CLASS_ID = "010";

    /**
     * World Data
     **/
    private static ClientEventRegistrar instance;
    final Map<Integer, EventPriority> PRIORITIES = new HashMap<>();
    
    final Set<Consumer<ClientStartedEvent>> ON_CLIENT_STARTED_EVENT = new ConcurrentSet<>();
    final Map<TickScheme, Consumer<?>> CLIENT_TICK_EVENTS = new ConcurrentHashMap<>();
    final Map<TickScheme, Consumer<?>> CLIENT_LEVEL_TICK_EVENTS = new ConcurrentHashMap<>();


    /**
     * Constructor
     **/
    private ClientEventRegistrar() {
        super();
        LoggerBase.logInit(null, "010000", this.getClass().getName());

        instance = this;
    }

    public static ClientEventRegistrar getInstance() {
        return instance;
    }

    public static void init() {
        instance = new ClientEventRegistrar();
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


    public void registerOnClientStarted(Consumer<ClientStartedEvent> function) {
        registerOnClientStarted(function, EventPriority.Normal);
    }

    public void registerOnClientStarted(Consumer<ClientStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_STARTED_EVENT, priority);
    }


    //** TICK EVENTS
    @SuppressWarnings("unchecked")
    public <T extends ClientTickEvent> void registerOnClientTick(TickType type, Consumer<T> function) {
        registerOnClientTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientTickEvent> void registerOnClientTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, CLIENT_TICK_EVENTS, type, priority);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientLevelTickEvent> void registerOnClientLevelTick(TickType type, Consumer<T> function) {
        registerOnClientLevelTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientLevelTickEvent> void registerOnClientLevelTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, CLIENT_LEVEL_TICK_EVENTS, type, priority);
    }

    /**
     * Custom Events
     **/


    public void onServerLevelTick(Level level) {
        if( level == null ) return;
        ManagedChunkEvents.onWorldTickStart(level);
    }

    public void onClientTick(Minecraft client) {
        long totalTicks = GeneralConfig.getInstance().getTotalTickCount();
        ClientTickEvent event = new ClientTickEvent(totalTicks);
        //LoggerBase.logDebug(null, "010001", "Client tick event: " + totalTicks);
        CLIENT_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (totalTicks % scheme.getFrequency() == scheme.offset) {
                tryEvent((Consumer<ClientTickEvent>) consumer, event);
            }
        });
    }

    public void onClientLevelTick(Level level) {
        long totalTicks = GeneralConfig.getInstance().getTotalTickCount();
        ClientLevelTickEvent event = new ClientLevelTickEvent(level, totalTicks);
        if(level == null) return;

        ManagedChunkEvents.onWorldTickStart(level);
        //LoggerBase.logDebug(null, "010001", "Client level tick event: " + totalTicks);
        CLIENT_LEVEL_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (totalTicks % scheme.getFrequency() == scheme.offset) {
                tryEvent((Consumer<ClientLevelTickEvent>) consumer, event);
            }
        });
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
