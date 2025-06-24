package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.BreakBlockEvent;
import net.blay09.mods.balm.api.event.PlayerAttackEvent;
import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;

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

    final Set<Consumer<ClientStartedEvent>> ON_CLIENT_STARTED_EVENT = new ConcurrentSet<>();

    final Set<Consumer<ServerStartingEvent>> ON_BEFORE_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStartedEvent>> ON_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStoppedEvent>> ON_SERVER_STOP = new ConcurrentSet<>();
    final Set<Consumer<ConnectedToServerEvent>> ON_CONNECTED_TO_SERVER = new ConcurrentSet<>();
    final Set<Consumer<DisconnectedFromServerEvent>> ON_DISCONNECTED_FROM_SERVER = new ConcurrentSet<>();

    final Set<Consumer<ServerTickEvent.SingleTick>> ON_SINGLE_TICK = new ConcurrentSet<>();
    final Set<Consumer<ServerTickEvent.Every20Ticks>> ON_20_TICKS = new ConcurrentSet<>();
    final Set<Consumer<ServerTickEvent.Every120Ticks>> ON_120_TICKS = new ConcurrentSet<>();
    final Set<Consumer<ServerTickEvent.Every1200Ticks>> ON_1200_TICKS = new ConcurrentSet<>();
    final Set<Consumer<ServerTickEvent.Every1200Ticks>> ON_6000_TICKS = new ConcurrentSet<>();
    final Set<Consumer<ServerTickEvent.DailyTick>> ON_DAILY_TICK = new ConcurrentSet<>();

    final Set<Consumer<DatastoreSaveEvent>> ON_DATA_SAVE = new ConcurrentSet<>();
    final Set<Consumer<PlayerAttackEvent>> ON_PLAYER_ATTACK = new ConcurrentSet<>();
    final Set<Consumer<BreakBlockEvent>> ON_BLOCK_BROKEN = new ConcurrentSet<>();
    final Set<Consumer<PlayerChangedDimensionEvent>> ON_PLAYER_CHANGED_DIMENSION = new ConcurrentSet<>();



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
        DatastoreSaveEvent event = DatastoreSaveEvent.create();
        for (Consumer<DatastoreSaveEvent> saver : ON_DATA_SAVE) {
            saver.accept(event);
        }
        event.getDataStore().save();
    }

    public void onServerTick(MinecraftServer server)
    {
        long totalTicks = GeneralConfig.getInstance().getTotalTickCount();
        
        // Fire single tick event
        ServerTickEvent.SingleTick singleTickEvent = new ServerTickEvent.SingleTick(totalTicks);
        for (Consumer<ServerTickEvent.SingleTick> consumer : ON_SINGLE_TICK) {
            consumer.accept(singleTickEvent);
        }
        //** TICKS OFFSET TO AVOID ALL OCCURRING AT THE SAME TIME **//

        // Fire every 20 ticks
        if ((totalTicks+1) % 20 == 0) {
            ServerTickEvent.Every20Ticks event20 = new ServerTickEvent.Every20Ticks(totalTicks);
            for (Consumer<ServerTickEvent.Every20Ticks> consumer : ON_20_TICKS) {
                consumer.accept(event20);
            }
        }

        // Fire every 120 ticks
        if ((totalTicks+2) % 120 == 0) {
            ServerTickEvent.Every120Ticks event120 = new ServerTickEvent.Every120Ticks(totalTicks);
            for (Consumer<ServerTickEvent.Every120Ticks> consumer : ON_120_TICKS) {
                consumer.accept(event120);
            }
        }

        // Fire every 1200 ticks
        if ((totalTicks+3) % 1200 == 0) {
            ServerTickEvent.Every1200Ticks event1200 = new ServerTickEvent.Every1200Ticks(totalTicks);
            for (Consumer<ServerTickEvent.Every1200Ticks> consumer : ON_1200_TICKS) {
                consumer.accept(event1200);
            }
        }

        // Fire every 6000 ticks
        if ((totalTicks+4) % 6000 == 0) {
            ServerTickEvent.Every1200Ticks event6000 = new ServerTickEvent.Every1200Ticks(totalTicks);
            for (Consumer<ServerTickEvent.Every1200Ticks> consumer : ON_6000_TICKS) {
                consumer.accept(event6000);
            }
        }

        // Fire daily tick (24000 ticks = 1 minecraft day)
        if (totalTicks+5 % 24000 == 0) {
            ServerTickEvent.DailyTick eventDaily = new ServerTickEvent.DailyTick(totalTicks);
            for (Consumer<ServerTickEvent.DailyTick> consumer : ON_DAILY_TICK) {
                consumer.accept(eventDaily);
            }
        }
    }

    /** ############### **/



    //Create public methods for pushing functions onto each function event
    private <T> void generalRegister(Consumer<T> function, Set<Consumer<T>> set, EventPriority priority) {
        set.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function) { registerOnPlayerLogin(function, EventPriority.Normal);}
    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGIN, priority);
    }

    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function) { registerOnPlayerLogout(function, EventPriority.Normal);}
    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGOUT, priority);
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

    public void registerOnConnectedToServer(Consumer<ConnectedToServerEvent> function) { registerOnConnectedToServer(function, EventPriority.Normal);}
    public void registerOnConnectedToServer(Consumer<ConnectedToServerEvent> function, EventPriority priority) {
        generalRegister(function, ON_CONNECTED_TO_SERVER, priority);
    }

    public void registerOnDisconnectedFromServer(Consumer<DisconnectedFromServerEvent> function) { registerOnDisconnectedFromServer(function, EventPriority.Normal);}
    public void registerOnDisconnectedFromServer(Consumer<DisconnectedFromServerEvent> function, EventPriority priority) {
        generalRegister(function, ON_DISCONNECTED_FROM_SERVER, priority);
    }


    public enum TickType {
        ON_SINGLE_TICK,
        ON_20_TICKS,
        ON_120_TICKS,
        ON_1200_TICKS,
        ON_6000_TICKS,
        DAILY_TICK
    }

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function) {
        registerOnServerTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function, EventPriority priority) {
        switch (type) {
            case ON_SINGLE_TICK:
                generalRegister((Consumer<ServerTickEvent.SingleTick>) function, ON_SINGLE_TICK, priority);
                break;
            case ON_20_TICKS:
                generalRegister((Consumer<ServerTickEvent.Every20Ticks>) function, ON_20_TICKS, priority);
                break;
            case ON_120_TICKS:
                generalRegister((Consumer<ServerTickEvent.Every120Ticks>) function, ON_120_TICKS, priority);
                break;
            case ON_1200_TICKS:
                generalRegister((Consumer<ServerTickEvent.Every1200Ticks>) function, ON_1200_TICKS, priority);
                break;
            case ON_6000_TICKS:
                generalRegister((Consumer<ServerTickEvent.Every1200Ticks>) function, ON_6000_TICKS, priority);
                break;
            case DAILY_TICK:
                generalRegister((Consumer<ServerTickEvent.DailyTick>) function, ON_DAILY_TICK, priority);
                break;
        }
    }

    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function) { registerOnDataSave(function, EventPriority.Normal);}
    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function, EventPriority priority) {
        generalRegister(function, ON_DATA_SAVE, priority);
    }

    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function) { registerOnPlayerAttack(function, EventPriority.Normal); }
    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_ATTACK, priority);
    }

    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function) { registerOnBreakBlock(function, EventPriority.Normal); }
    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function, EventPriority priority) {
        generalRegister(function, ON_BLOCK_BROKEN, priority);
    }

    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function) { registerOnPlayerChangedDimension(function, EventPriority.Normal); }
    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_CHANGED_DIMENSION, priority);
    }


}
//END CLASS
