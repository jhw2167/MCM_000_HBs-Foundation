package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.WakeUpAllPlayersEvent;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
    private boolean isClientSide;
    private boolean isServerSide;

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

        reg.registerOnBeforeServerStarted(instance::onBeforeServerStarted, EventPriority.Highest);
        reg.registerOnServerStopped(instance::onServerStopped, EventPriority.Lowest);

        reg.registerOnLevelLoad(instance::onLoadLevel, EventPriority.Highest);
        reg.registerOnLevelUnload(instance::onUnLoadLevel, EventPriority.Lowest);
        reg.registerOnWakeUpAllPlayers(instance::onWakeUpAllPlayers, EventPriority.Highest);
    }

    private void onWakeUpAllPlayers(WakeUpAllPlayersEvent event) {
        LevelSaveData lsd = dataStore.getOrCreateLevelSaveData(Constants.MOD_ID, event.getLevel());
        int newTotal = event.getTotalSleeps() + 1;
        lsd.addProperty("totalSleeps", new JsonPrimitive(newTotal));
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

    public void onBeforeServerStarted(ServerStartingEvent event) {
        this.isServerSide = true;
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
        this.isClientSide = false;
        this.isServerSide = false;
    }

    /** Level Events **/

    public  static final ResourceLocation OVERWORLD_LOC = new ResourceLocation("minecraft", "overworld");
    public static final ResourceLocation NETHER_LOC = new ResourceLocation("minecraft", "the_nether");
    public static final ResourceLocation END_LOC = new ResourceLocation("minecraft", "the_end");
    public static ServerLevel OVERWORLD;
    public static ServerLevel NETHER;
    public static ServerLevel END;
    public void onLoadLevel(LevelLoadingEvent.Load event)
    {
        Level level = (Level) event.getLevel();
        this.LEVELS.put(HBUtil.LevelUtil.toLevelId(level), level);
        LevelData data = level.getLevelData();
        BlockPos spawn = new BlockPos(data.getXSpawn(), data.getYSpawn(), data.getZSpawn());
        this.WORLD_SPAWNS.put(level, spawn);
        //if( level.isClientSide() ) return;

        if (level instanceof ServerLevel serverLevel)
        {
            if (HBUtil.LevelUtil.testLevel(level, OVERWORLD_LOC)) {
                OVERWORLD = serverLevel;
            } else if (HBUtil.LevelUtil.testLevel(level, NETHER_LOC)) {
                NETHER = serverLevel;
            } else if (HBUtil.LevelUtil.testLevel(level, END_LOC)) {
                END = serverLevel;
            }
        }
    }

    public void onUnLoadLevel(LevelLoadingEvent.Unload event)  {
        if(event.getLevel().isClientSide()) return;

        LevelSaveData lsd = dataStore.getOrCreateLevelSaveData(Constants.MOD_ID, event.getLevel());
        long gameTime = this.getTotalTickCountWithSleep((Level) event.getLevel());
        lsd.addProperty("totalTicksWithSleep", new JsonPrimitive(gameTime));
    }


    public boolean isWorldConfigInit() {
        return this.isWorldConfigInit;
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

    public boolean isClientSide() {
        return this.isClientSide;
    }
    public boolean isServerSide() {
        return this.isServerSide;
    }

    public boolean isIntegrated() {
        return this.isServerSide && this.isClientSide;
    }


    @Nullable
    public MinecraftServer getServer() {
        return server;
    }



    public long getTotalTickCount() {
        if(this.isServerSide)
            return dataStore.getTotalTickCount() + server.getTickCount();
        else return 0;
    }

    public static long TICKS_PER_DAY = 24000;
    public long getTotalTickCountWithSleep(Level level) {
        int totalSleeps = this.getTotalSleeps(level);
        return (TICKS_PER_DAY * totalSleeps) + level.getDayTime();
    }

    public long getSessionTickCount() {
        if(this.isServerSide)
            return server.getTickCount();
        return -1;
    }

    public int getTotalSleeps(Level level) {
        LevelSaveData lsd = dataStore.getOrCreateLevelSaveData(Constants.MOD_ID, level);
        return lsd.get("totalSleeps").getAsInt();
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


    //** SUBCLASSES


}
//END CLASS
