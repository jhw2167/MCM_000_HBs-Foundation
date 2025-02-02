package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.LoggerBase;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;


import java.util.*;
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
    
    final ArrayDeque<Consumer<PlayerLoginEvent>> ON_PLAYER_LOAD = new ArrayDeque<>();
    final Deque<Consumer<LevelLoadingEvent.Load>> ON_LEVEL_LOAD = new ArrayDeque<>();
    final Deque<Consumer<LevelLoadingEvent.Unload>> ON_LEVEL_UNLOAD = new ArrayDeque<>();


    final Deque<Consumer<ChunkLoadingEvent.Load>> ON_CHUNK_LOAD = new ArrayDeque<>();
    final Deque<Consumer<ChunkLoadingEvent.Unload>> ON_CHUNK_UNLOAD = new ArrayDeque<>();

    //final Deque<Consumer<ModLifecycleEvent>> ON_MOD_LIFECYCLE = new ArrayDeque<>();
        //Will have to divide up into different lifecycles

    //final Deque<Consumer<RegisterEvent>> ON_REGISTER = new ArrayDeque<>();
        //Dont see it, I think this is for registering commands

    //final Deque<Consumer<ModConfigEvent>> ON_MOD_CONFIG = new ArrayDeque<>();
        //Dont see it, is probably different for forge and fabric, Balm abstracts away all configuration

    final Deque<Consumer<ClientStartedEvent>> ON_CLIENT_STARTED_EVENT = new ArrayDeque<>();

    final Deque<Consumer<ServerStartingEvent>> ON_BEFORE_SERVER_START = new ArrayDeque<>();
    final Deque<Consumer<ServerStartedEvent>> ON_SERVER_START = new ArrayDeque<>();
    final Deque<Consumer<ServerStoppedEvent>> ON_SERVER_STOP = new ArrayDeque<>();
    final Deque<Runnable> ON_DATA_SAVE = new ArrayDeque<>();


    /**
     * Constructor
     **/
    private EventRegistrar()
    {
        super();
        LoggerBase.logInit( null, "010000", this.getClass().getName());

        instance = this;
    }

    public static EventRegistrar getInstance() {
        return instance;
    }

    public static void init() {
        instance = new EventRegistrar();
    }

    /** Custom Events **/

    public void dataSaveEvent()
    {
        for (Runnable function : ON_DATA_SAVE) {
            function.run();
        }
    }

    /** ############### **/



    //Create public methods for pushing functions onto each function event
    private <T> void  generalRegister(Consumer<T> function, Deque<Consumer<T>> array, EventPriority priority) {
        array.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnPlayerLoad(Consumer<PlayerLoginEvent> function) { registerOnPlayerLoad(function, EventPriority.Normal);}
    public void registerOnPlayerLoad(Consumer<PlayerLoginEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOAD, priority);
    }


    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function) { registerOnLevelLoad(function, EventPriority.Normal);}
    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_LOAD, priority);
    }


    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function) { registerOnLevelUnload(function, EventPriority.Normal);}
    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_UNLOAD, priority);
    }


    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function) { registerOnChunkLoad(function, EventPriority.Normal);}
    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_CHUNK_LOAD, priority);
    }

    public void registerOnChunkUnload(Consumer<ChunkLoadingEvent.Unload> function) { registerOnChunkUnload(function, EventPriority.Normal);}
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

    public void registerOnClientStarted(Consumer<ClientStartedEvent> function) { registerOnClientStarted(function, EventPriority.Normal);}
    public void registerOnClientStarted(Consumer<ClientStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_STARTED_EVENT, priority);
    }

    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function) { registerOnBeforeServerStarted(function, EventPriority.Normal);}
    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function, EventPriority priority) {
        generalRegister(function, ON_BEFORE_SERVER_START, priority);
    }


    public void registerOnServerStarted(Consumer<ServerStartedEvent> function) { registerOnServerStarted(function, EventPriority.Normal);}
    public void registerOnServerStarted(Consumer<ServerStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_START, priority);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function) { registerOnServerStopped(function, EventPriority.Normal);}
    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_STOP, priority);
    }

    public void registerOnDataSave(Runnable function) { registerOnDataSave(function, EventPriority.Normal);}
    public void registerOnDataSave(Runnable function, EventPriority priority) {
        if (priority == EventPriority.Highest )
            ON_DATA_SAVE.addFirst(function);
        else
            ON_DATA_SAVE.add(function);
    }


}
//END CLASS
