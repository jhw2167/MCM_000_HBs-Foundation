package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.datastore.WorldSaveData;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.model.ManagedChunk;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class GeneralConfig {
    public static final String CLASS_ID = "000";

    public static final Gson GSON = new Gson();

    /**
     * World Data
     **/
    private static GeneralConfig instance;
    private final DataStore DATA_STORE;
    private Thread watchThread;
    private volatile boolean running = true;

    private MinecraftServer SERVER;
    private final Map<String, LevelAccessor> LEVELS;
    private Long WORLD_SEED;
    private Vec3i WORLD_SPAWN;
    private Boolean PLAYER_LOADED;

    /**
     * Constructor
     **/
    private GeneralConfig()
    {
        super();
        LoggerBase.logInit( null, "000000", this.getClass().getName());
        this.PLAYER_LOADED = false;
        this.DATA_STORE = DataStore.getInstance();
        this.LEVELS = new HashMap<>();
        this.running = true;
        startWatchThread();

        instance = this;
    }

    public static GeneralConfig getInstance() {
        if (instance == null)
            return new GeneralConfig();
        return instance;
    }

    /**
     * Events
     */


    /** Level Events **/
    static {
        EventRegistrar reg = EventRegistrar.getInstance();
        reg.registerOnLevelLoad(GeneralConfig::onLoadLevel, true);
    }

    public static void onLoadLevel(LevelEvent.Load event)
    {
        // Capture the world seed, use logical server
        LevelAccessor level = event.getLevel();
        if( level.isClientSide() ) {
            instance.LEVELS.put(HBUtil.LevelUtil.toId(level), level);
        }
        else
        {
            MinecraftServer server = level.getServer();
            instance.WORLD_SEED = server.overworld().getSeed();
            instance.WORLD_SPAWN = server.overworld().getSharedSpawnPos();
            instance.LEVELS.put(HBUtil.LevelUtil.toId(level), level);
            instance.SERVER = level.getServer();

            initDataStore();
            createLevelData(level);

            LoggerBase.logInfo( null, "010001", "World Seed: " + instance.WORLD_SEED);
            LoggerBase.logInfo( null, "010002", "World Spawn: " + instance.WORLD_SPAWN);
        }

    }

    public void onUnLoadLevel(LevelEvent.Unload event)  {
        //not implemented
    }

    public void initPlayerConfigs(PlayerEvent.PlayerLoggedInEvent event)
    {
        PLAYER_LOADED = true;
        LoggerBase.logDebug( null,"006001", "Player Logged In");
    }

    /**
     * Getters
     */
    public LevelAccessor getLevel(String id) throws InvalidId
    {
        if(LEVELS == null)
            throw new InvalidId("LEVELS is null");

        LevelAccessor level = LEVELS.get(id);
        if( level == null)
            throw new InvalidId("Level ID not found in GeneralConfig.LEVELS: " + id);
        return level;
    }

    public Long getWORLD_SEED() {
        return WORLD_SEED;
    }

    public Vec3i getWORLD_SPAWN() {
        return WORLD_SPAWN;
    }

    public Boolean getIsPLAYER_LOADED() {
        return PLAYER_LOADED;
    }

    public MinecraftServer getSERVER() {
        return SERVER;
    }


    /**
     * Setters
     */




    /**
     * Utility
     */

     private static void initDataStore()
     {
        DataStore.getInstance().initWorldOnLevelLoad(instance);
     }

     private static void createLevelData(LevelAccessor level)
     {
         WorldSaveData worldData = DataStore.getInstance().getOrCreateWorldSaveData(HBUtil.NAME);
         worldData.getOrCreateLevelSaveData(level);
     }


    /**
     * SAVE METHODS - register class to save method
     */

    private void startWatchThread() {
        watchThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(30000); // 30 seconds
                    if (running) {
                        EventRegistrar.getInstance().dataSaveEvent();
                    }
                } catch (InterruptedException e) {
                    // Interrupted, exit the thread
                    break;
                }
            }
        });
        watchThread.setDaemon(true);
        watchThread.setName("GeneralConfig-AutoSave");
        watchThread.start();
    }

    public void shutdown() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                // Ignore interruption during shutdown
            }
        }
    }

}
//END CLASS
