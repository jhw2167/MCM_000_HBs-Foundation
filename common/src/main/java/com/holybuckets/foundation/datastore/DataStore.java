package com.holybuckets.foundation.datastore;

import com.google.gson.*;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.holybuckets.foundation.GeneralConfig.TICKS_PER_DAY;

/**
 * Class DataStore - manages a simple persisted json file used as a datastore that holds
 * world and save file level data on the server side. To be read into memory at startup and serialized
 * periodically
 */
public class DataStore implements IStringSerializable {

    private static final String CLASS_ID = "007";

    private static DataStore INSTANCE;
    private static final File DATA_STORE_FILE = new File("hb_datastore.json");
    private final Map<String, ModSaveData> STORE;
    private String currentWorldId;
    private long totalTickCountFromPreviousSession;

    private DataStore() {
        super();
        this.currentWorldId = null;
        STORE = new HashMap<>();
    }

    public static DataStore init() {
        INSTANCE = new DataStore();
        return INSTANCE;
    }


    private void loadData(Path worldPath)
    {
        this.currentWorldId = worldPath.getParent().getFileName().toString();
        File dataStoreFile = new File(worldPath.toFile(), DATA_STORE_FILE.getName());
        String json = HBUtil.FileIO.loadJsonConfigs(dataStoreFile, DATA_STORE_FILE, new DefaultDataStore());
        this.deserialize(json);
        this.write();
    }


    // Constructor for default data store
    private DataStore(ModSaveData data)
    {
        super();
        STORE = new HashMap<>();
        STORE.put(data.getModId(), data);
    }


    public ModSaveData getOrCreateModSavedData(String modId) {
        ModSaveData data = STORE.getOrDefault(modId, new ModSaveData(modId));
        STORE.put(modId, data);
        return data;
    }


    public WorldSaveData getOrCreateWorldSaveData(String modId) {
        ModSaveData modData = getOrCreateModSavedData(modId);
        return modData.getOrCreateWorldSaveData(currentWorldId);
    }

    public LevelSaveData getOrCreateLevelSaveData(String modId, Level level) {
        WorldSaveData worldData = getOrCreateWorldSaveData(modId);
        return worldData.getOrCreateLevelSaveData(level);
    }


    /**
     * Initialize a worldSaveData object on HB's Utility when a level loads
     * @param event - server started
     * @return true if the worldSaveData object was initialized, false if it already exists
     */
    public boolean initWorldDataOnServerStart(ServerStartingEvent event)
    {
        ModSaveData modData = getOrCreateModSavedData(Constants.MOD_ID);
        if (modData.worldSaveData.containsKey(currentWorldId)) {
            this.totalTickCountFromPreviousSession = modData.getOrCreateWorldSaveData(currentWorldId).get("totalTicks").getAsLong();
            return false;
        }

        GeneralConfig config = GeneralConfig.getInstance();
        while( config == null || !config.isWorldConfigInit() ){
            config = GeneralConfig.getInstance();
        }

        modData.getOrCreateWorldSaveData(currentWorldId);
        this.totalTickCountFromPreviousSession = 0L;
        return true;
    }

    private static JsonElement parse(Object o) {
        return JsonParser.parseString( GeneralConfig.GSON.toJson(o) );
    }

    /**
     * On Server Stopped, remove any world data from deleted worlds
     */
    private void removeDeletedWorldSaveData(LevelStorageSource lss) {
        //LevelStorageSource lss = event.server().file .getLevelSource();
        Set<String> worldNames = lss.findLevelCandidates().levels().stream().map( ld -> ld.directoryName()).collect(HashSet::new, HashSet::add, HashSet::addAll);
        if(worldNames.isEmpty()) return;
        STORE.forEach((modId, modData) -> {
            modData.worldSaveData.keySet().removeIf( worldId -> !worldNames.contains(worldId) );
        });
    }


    /** Serializers **/

    public void deserialize(String jsonString)
    {
        //test if the string contains valid json
        if(jsonString == null || jsonString.isEmpty())
            return;

        String malformedJson = null;
        //test if JSON is not structured properly
        try {
            JsonParser.parseString(jsonString);
        } catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error parsing JSON data from Datastore, file: ");
            error.append(DATA_STORE_FILE);
            error.append(".  The datastore will be configured with defaults values. Copy the file to save your data. ");

            malformedJson = new String(jsonString);
            jsonString = new DefaultDataStore().serialize();
        }


        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonArray modSaveDataArray = json.getAsJsonArray("modSaveData");
            if (modSaveDataArray == null || !modSaveDataArray.isJsonArray()) {
                throw new JsonParseException("modSaveData is missing or not an array");
            }
            modSaveDataArray.forEach(modSaveData -> {
                ModSaveData data = new ModSaveData(modSaveData.getAsJsonObject());
                STORE.put(data.getModId(), data);
            });
        } catch (Exception e) {
            malformedJson = jsonString;
            LoggerBase.logError(null, "007011" , "Error processing mod save data, entire suspect data written out to hb_datastore.json");
        }


        if( malformedJson != null)
        {
            ModSaveData utilData = STORE.get(Constants.MOD_ID);
            String asString = malformedJson.replace('"','\'' ).replaceAll("\r\n", "");
            utilData.addProperty("malformedJson",parse( asString ) );
            this.write();
        }

    }

    public String serialize() {
        JsonObject json = new JsonObject();
        JsonArray modSaveDataArray = new JsonArray();
        STORE.forEach((modId, data) -> {
            modSaveDataArray.add(data.toJson());
        });
        json.add("modSaveData", modSaveDataArray);
        return json.toString();
    }

    public void write() {
        if(this.currentWorldPath == null) return;
        HBUtil.FileIO.serializeJsonConfigs(DATA_STORE_FILE, this.serialize());
    }

    //** EVENTS


    public void onBeforeServerStarted(ServerStartingEvent event) {
        MinecraftServer s = event.getServer();
        Path path = s.getWorldPath( LevelResource.ROOT );

        this.loadData( path );
        this.initWorldDataOnServerStart(event);
    }

    public void onServerStopped( ServerStoppedEvent s ) {
        this.shutdown(s);
    }

    public void shutdown(ServerStoppedEvent s)
    {
        try {

            WorldSaveData worldData = this.getOrCreateWorldSaveData(Constants.MOD_ID);
            Long currentTicks = GeneralConfig.getInstance().getTotalTickCount();
            worldData.addProperty("totalTicks", parse(currentTicks) );
            this.write();
        } catch (Exception e) {
            // If the world data is not initialized, we cannot save it
            return;
        }

    }

    /** STATIC METHODS **/

    //write a public static method to retrieve the world properties
    public static Long getTotalTickCount() {
        ModSaveData modData = INSTANCE.getOrCreateModSavedData(Constants.MOD_ID);
        if (!modData.worldSaveData.containsKey(INSTANCE.currentWorldId))
            return -1L;

        WorldSaveData worldData = modData.getOrCreateWorldSaveData(INSTANCE.currentWorldId);
        return worldData.get("totalTicks").getAsLong();
    }

    /**
     * Total world ticks from before the last server shutdown
     * @return Long total ticks
     */
    public static Long getTotalTicksFromPreviousSession() {
        return INSTANCE.totalTickCountFromPreviousSession;
    }


    //Initialize default properties
    private static void initLevelSaveData(LevelSaveData lsd, LevelAccessor l)
    {
        String id = HBUtil.LevelUtil.toLevelId(l);
        lsd.addProperty("levelId", new JsonPrimitive(id));
        lsd.addProperty("totalSleeps", new JsonPrimitive(0));
        lsd.addProperty("totalTicksWithSleep", new JsonPrimitive(0));
        lsd.addProperty("totalDays", new JsonPrimitive(0));
        long nextDailyTick = l.dimensionType().fixedTime().orElse(TICKS_PER_DAY);
        lsd.addProperty("nextDailyTick", new JsonPrimitive(nextDailyTick));
    }




    /** SUBCLASSES **/

    private class DefaultDataStore extends DataStore implements IStringSerializable {
        public static final ModSaveData DATA = new ModSaveData(Constants.MOD_ID);
        static {
            DATA.setComment("The purpose of this JSON file is to store data at the world save file level for supporting HB" +
             " Utility Foundation mods. This data is not intended to be modified by the user.");
        }
        public DefaultDataStore() {
            super(DATA);
        }

        @Override
        public String serialize() {
            return super.serialize();
        }

        @Override
        public void deserialize(String jsonString) {
            //Dummy
        }
    }


}
