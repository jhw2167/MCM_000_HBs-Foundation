package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import com.google.gson.Gson;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
    private ExecutorService watchExecutorService;
    private volatile boolean running = true;
    private Boolean isClientSide;

    private MinecraftServer server;
    private final Map<String, LevelAccessor> LEVELS;
    private Map<LevelAccessor, Vec3i> WORLD_SPAWNS;
    private Long worldSeed;
    private boolean isWorldConfigInit;
    private Boolean isPlayerLoaded;
    private PerformanceImpactConfig performanceImpactConfig;

    /**
     * Constructor
     **/
    private GeneralConfig()
    {
        super();
        instance = this;
        LoggerBase.logInit( null, "000000", this.getClass().getName());
        this.isClientSide = false;
        this.isPlayerLoaded = false;
        this.LEVELS = new HashMap<>();
        this.WORLD_SPAWNS = new HashMap<>();
        this.running = true;
        this.isWorldConfigInit = false;
    }

    public static void init(EventRegistrar reg)
    {
        if (instance != null)
            return;
        instance = new GeneralConfig();
        instance.dataStore = DataStore.init();

        reg.registerOnConnectedToServer(instance::onPlayerLoginToServerEvent, EventPriority.Highest);
        reg.registerOnDisconnectedFromServer(instance::onPlayerDisconnectedFromServerEvent, EventPriority.Lowest);

        reg.registerOnBeforeServerStarted(instance::onBeforeServerStarted, EventPriority.Highest);
        reg.registerOnServerStopped(instance::onServerStopped, EventPriority.Lowest);

        reg.registerOnLevelLoad(instance::onLoadLevel, EventPriority.Highest);
        reg.registerOnLevelUnload(instance::onUnLoadLevel, EventPriority.Lowest);

        reg.registerOnPlayerLogin(instance::initPlayerConfigs, EventPriority.Highest);

    }

    private void initPerformanceConfig() {
        this.performanceImpactConfig = new PerformanceImpactConfig();
    }


    public static GeneralConfig getInstance() {
        return instance;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public Map<String,LevelAccessor> getLevels() {
        return new HashMap<>(LEVELS);
    }


    /** Server Events **/

    public void onPlayerLoginToServerEvent(ConnectedToServerEvent event) {
        this.isClientSide = true;
        this.initPerformanceConfig();
    }

    public void onPlayerDisconnectedFromServerEvent(DisconnectedFromServerEvent event) {
        this.isClientSide = null;
    }

    public void onBeforeServerStarted(ServerStartingEvent event) {
        this.isClientSide = false;
        this.initPerformanceConfig();


        if( this.server == null )
            this.server = event.getServer();

        if( !this.isWorldConfigInit)
        {
            this.worldSeed = server.getWorldData().worldGenOptions().seed();

            if( this.worldSeed != null ) {
                this.isWorldConfigInit = true;
                LoggerBase.logInfo( null, "010001", "World Seed: " + this.worldSeed);
            }

        }

        this.dataStore = DataStore.init();
        this.dataStore.onBeforeServerStarted(event);
        this.watchExecutorService = Executors.newSingleThreadExecutor();
        this.watchExecutorService.submit(this::startAutoSaveThread);
    }

    public void onServerStopped(ServerStoppedEvent event) {
        if( this.dataStore == null) return;
        this.dataStore.onServerStopped(event);
        this.dataStore = null;
        this.server = null;
        this.isClientSide = null;
    }

    /** Level Events **/

    public void onLoadLevel(LevelLoadingEvent.Load event)
    {
        LevelAccessor level = event.getLevel();
        this.LEVELS.put(HBUtil.LevelUtil.toLevelId(level), level);
        LevelData data = level.getLevelData();
        BlockPos spawn = new BlockPos(data.getXSpawn(), data.getYSpawn(), data.getZSpawn());
        this.WORLD_SPAWNS.put(level, spawn);
        //if( level.isClientSide() ) return;
    }

    public void onUnLoadLevel(LevelLoadingEvent.Unload event)  {
        //not implemented
    }


    public boolean isWorldConfigInit() {
        return this.isWorldConfigInit;
    }

    public void initPlayerConfigs(PlayerLoginEvent event)
    {
        isPlayerLoaded = true;
        Player p = event.getPlayer();
        LoggerBase.logDebug( null,"006001", "Player Logged In " + p.getName() + " | " + p.getUUID() );
    }

    /**
     * Getters
     */

    /**
     * Get Level by from levelId - package private, user should user HBUtil.LevelUtil to access
     * @param id
     * @return LevelAccessor either serverLevel or clientLevel
     */
     @Nullable
    LevelAccessor getLevel(String id)
    {
        if(LEVELS == null) return null;
        LevelAccessor level = LEVELS.get(id);
        if( level == null) return null;
        return level;
    }

    public Long getWorldSeed() {
        return worldSeed;
    }

    public Vec3i getWorldSpawn(LevelAccessor level) {
        return WORLD_SPAWNS.get(level);
    }

    public Boolean getIsPLAYER_LOADED() {
        return isPlayerLoaded;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public long getTotalTickCount() {
        return dataStore.getTotalTickCount() + server.getTickCount();
    }

    public PerformanceImpactConfig getPerformanceImpactConfig() {
        if(this.performanceImpactConfig == null)
            this.initPerformanceConfig();
        return performanceImpactConfig;
    }

    /**
     * Setters
     */



    /**
     * SAVE METHODS - register class to save method
     */

    private void startAutoSaveThread()
    {
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
    }

    public void stopAutoSaveThread()
    {
        try {
            this.watchExecutorService.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            this.watchExecutorService.shutdownNow();
        } catch (InterruptedException e) {
            LoggerBase.logError(null, "006002", "Error stopping AutoSaveThread");
            }
        }

    }
//END CLASS
