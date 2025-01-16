package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.LoggerBase;
import net.blay09.mods.balm.api.event.ChunkEvent;
import net.blay09.mods.balm.api.event.LevelEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
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
    
    final Deque<Consumer<PlayerLoginEvent>> ON_PLAYER_LOAD = new ArrayDeque<>();
    final Deque<Consumer<LevelEvent.Load>> ON_LEVEL_LOAD = new ArrayDeque<>();
    final Deque<Consumer<LevelEvent.Unload>> ON_LEVEL_UNLOAD = new ArrayDeque<>();


    final Deque<Consumer<ChunkEvent.Load>> ON_CHUNK_LOAD = new ArrayDeque<>();
    final Deque<Consumer<ChunkEvent.Unload>> ON_CHUNK_UNLOAD = new ArrayDeque<>();

    //final Deque<Consumer<ModLifecycleEvent>> ON_MOD_LIFECYCLE = new ArrayDeque<>();
        //Will have to divide up into different lifecycles

    //final Deque<Consumer<RegisterEvent>> ON_REGISTER = new ArrayDeque<>();
        //Dont see it, I think this is for registering commands

    //final Deque<Consumer<ModConfigEvent>> ON_MOD_CONFIG = new ArrayDeque<>();
        //Dont see it, is probably different for forge and fabric, Balm abstracts away all configuration

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
    public <T> void  generalRegister(Consumer<T> function, Deque<Consumer<T>> array, boolean priority) {
        if (priority)
            array.addFirst(function);
        else
            array.add(function);
    }

    public void registerOnPlayerLoad(Consumer<PlayerLoginEvent> function) { registerOnPlayerLoad(function, false);}
    public void registerOnPlayerLoad(Consumer<PlayerLoginEvent> function, boolean priority) {
        generalRegister(function, ON_PLAYER_LOAD, priority);
    }


    public void registerOnLevelLoad(Consumer<LevelEvent.Load> function) { registerOnLevelLoad(function, false);}
    public void registerOnLevelLoad(Consumer<LevelEvent.Load> function, boolean priority) {
        generalRegister(function, ON_LEVEL_LOAD, priority);
    }


    public void registerOnLevelUnload(Consumer<LevelEvent.Unload> function) { registerOnLevelUnload(function, false);}
    public void registerOnLevelUnload(Consumer<LevelEvent.Unload> function, boolean priority) {
        generalRegister(function, ON_LEVEL_UNLOAD, priority);
    }


    public void registerOnChunkLoad(Consumer<ChunkEvent.Load> function) { registerOnChunkLoad(function, false);}
    public void registerOnChunkLoad(Consumer<ChunkEvent.Load> function, boolean priority) {
        generalRegister(function, ON_CHUNK_LOAD, priority);
    }

    public void registerOnChunkUnload(Consumer<ChunkEvent.Unload> function) { registerOnChunkUnload(function, false);}
    public void registerOnChunkUnload(Consumer<ChunkEvent.Unload> function, boolean priority) {
        generalRegister(function, ON_CHUNK_UNLOAD, priority);
    }

    /*
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function) { registerOnModLifecycle(function, false); }
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function, boolean priority) {
        generalRegister(function, ON_MOD_LIFECYCLE, priority);
    }

    public void registerOnRegister(Consumer<RegisterEvent> function) { registerOnRegister(function, false); }
    public void registerOnRegister(Consumer<RegisterEvent> function, boolean priority) {
        generalRegister(function, ON_REGISTER, priority);
    }

    public void registerOnModConfig(Consumer<ModConfigEvent> function) { registerOnModConfig(function, false); }
    public void registerOnModConfig(Consumer<ModConfigEvent> function, boolean priority) {
        generalRegister(function, ON_MOD_CONFIG, priority);
    }
    */

    public void registerOnServerStarted(Consumer<ServerStartedEvent> function) { registerOnServerStarted(function, false); }
    public void registerOnServerStarted(Consumer<ServerStartedEvent> function, boolean priority) {
        generalRegister(function, ON_SERVER_START, priority);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function) { registerOnServerStopped(function, false); }
    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function, boolean priority) {
        generalRegister(function, ON_SERVER_STOP, priority);
    }

    public void registerOnDataSave(Runnable function) { registerOnDataSave(function, false); }
    public void registerOnDataSave(Runnable function, boolean priority) {
        if (priority)
            ON_DATA_SAVE.addFirst(function);
        else
            ON_DATA_SAVE.add(function);
    }


}
//END CLASS
