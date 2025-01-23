package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import com.google.gson.Gson;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.InvalidId;
import net.blay09.mods.balm.api.event.LevelEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.server.ServerBeforeStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;
import java.util.Map;


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
    private DataStore dataStore;
    private Thread watchThread;
    private volatile boolean running = true;

    private MinecraftServer server;
    private final Map<String, LevelAccessor> LEVELS;
    private Long worldSeed;
    private Vec3i worldSpawn;
    private boolean isLevelConfigInit;
    private Boolean isPlayerLoaded;

    /**
     * Constructor
     **/
    private GeneralConfig()
    {
        super();
        LoggerBase.logInit( null, "000000", this.getClass().getName());
        this.isPlayerLoaded = false;
        this.LEVELS = new HashMap<>();
        this.running = true;
        this.isLevelConfigInit = false;

        instance = this;
    }

    public static void init(EventRegistrar reg)
    {
        if (instance != null)
            return;
        instance = new GeneralConfig();
        instance.dataStore = DataStore.init();

        reg.registerOnBeforeServerStarted(instance::onBeforeServerStarted, true);
        reg.registerOnServerStopped(instance::onServerStopped, false);

        reg.registerOnLevelLoad(instance::onLoadLevel, true);
        reg.registerOnLevelUnload(instance::onUnLoadLevel, false);

        reg.registerOnPlayerLoad(instance::initPlayerConfigs, true);

    }

    public static GeneralConfig getInstance() {
        return instance;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    /** Server Events **/

    public void onBeforeServerStarted(ServerBeforeStartingEvent event) {
        this.dataStore = DataStore.init();
        this.dataStore.onBeforeServerStarted(event);
        this.startWatchAutoSaveThread();
    }

    public void onServerStopped(ServerStoppedEvent event) {
        this.dataStore.onServerStopped(event);
        this.dataStore = null;
    }

    /** Level Events **/

    public void onLoadLevel(LevelEvent.Load event)
    {
        LevelAccessor level = event.getLevel();
        this.LEVELS.put(HBUtil.LevelUtil.toId(level), level);

        if( level.isClientSide() )
            return;

        if( this.server == null )
            this.server = level.getServer();

        if( !this.isLevelConfigInit )
        {

            this.worldSeed = server.overworld().getSeed();
            this.worldSpawn = server.overworld().getSharedSpawnPos();

            if( this.worldSeed != null && this.worldSpawn != null ) {
                this.isLevelConfigInit = true;
                LoggerBase.logInfo( null, "010001", "World Seed: " + this.worldSeed);
                LoggerBase.logInfo( null, "010002", "World Spawn: " + this.worldSpawn);
            }

        }

    }

    public void onUnLoadLevel(LevelEvent.Unload event)  {
        //not implemented
    }


    public boolean isLevelConfigInit() {
        return this.isLevelConfigInit;
    }

    public void initPlayerConfigs(PlayerLoginEvent event)
    {
        isPlayerLoaded = true;
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

    public Long getWorldSeed() {
        return worldSeed;
    }

    public Vec3i getWorldSpawn() {
        return worldSpawn;
    }

    public Boolean getIsPLAYER_LOADED() {
        return isPlayerLoaded;
    }

    public MinecraftServer getServer() {
        return server;
    }


    /**
     * Setters
     */



    /**
     * SAVE METHODS - register class to save method
     */

    private void startWatchAutoSaveThread() {
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

    public void stopAutoSaveThread()
    {
        this.running = false;
        if (this.watchThread != null) {
            this.watchThread.interrupt();
            try {
                this.watchThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                // Ignore interruption during shutdown
            }
        }
    }

}
//END CLASS
