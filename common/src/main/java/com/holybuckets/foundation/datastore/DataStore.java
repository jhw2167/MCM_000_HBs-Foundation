package com.holybuckets.foundation.datastore;

import com.google.gson.*;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    private DataStore() {
        super();
        this.currentWorldId = null;
        STORE = new HashMap<>();
    }

    public static DataStore init() {
        INSTANCE = new DataStore();
        return INSTANCE;
    }


    private void loadData(String worldId)
    {
        this.currentWorldId = worldId;
        String json = HBUtil.FileIO.loadJsonConfigs(DATA_STORE_FILE, DATA_STORE_FILE, new DefaultDataStore());
        this.deserialize(json);
        this.save();
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

    public LevelSaveData getOrCreateLevelSaveData(String modId, LevelAccessor level) {
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
        if (modData.worldSaveData.containsKey(currentWorldId))
            return false;

        GeneralConfig config = GeneralConfig.getInstance();
        while( config == null || !config.isWorldConfigInit() ){
            config = GeneralConfig.getInstance();
        }

        WorldSaveData worldData = modData.getOrCreateWorldSaveData(currentWorldId);
        worldData.addProperty("worldSeed", parse(config.getWorldSeed()) );
        worldData.addProperty("totalTicks", parse(Integer.valueOf(0)) );

        return true;
    }
    private static JsonElement parse(Object o) {
        return JsonParser.parseString( GeneralConfig.GSON.toJson(o) );
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


        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray modSaveDataArray = json.getAsJsonArray("modSaveData");
        modSaveDataArray.forEach(modSaveData -> {
            ModSaveData data = new ModSaveData(modSaveData.getAsJsonObject());
            STORE.put(data.getModId(), data);
        });

        if( malformedJson != null)
        {
            ModSaveData utilData = STORE.get(Constants.MOD_ID);
            String asString = malformedJson.replace('"','\'' ).replaceAll("\r\n", "");
            utilData.addProperty("malformedJson",parse( asString ) );
            this.save();
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

    public void save() {
        HBUtil.FileIO.serializeJsonConfigs(DATA_STORE_FILE, this.serialize());
    }


    /** STATIC METHODS **/

    public void onBeforeServerStarted(ServerStartingEvent serverStartedEvent) {
        this.initDatastoreOnServerStart(serverStartedEvent);
        new Thread( () -> {
            this.initWorldDataOnServerStart(serverStartedEvent);
        }).start();
    }

    private void initDatastoreOnServerStart(ServerStartingEvent event) {
        MinecraftServer s = event.getServer();
        Path path = s.getWorldPath( LevelResource.ROOT );
        this.loadData( path.getParent().getFileName().toString() );
    }

    public void onServerStopped( ServerStoppedEvent s ) {
        this.shutdown(s);
    }
    public void shutdown(ServerStoppedEvent s)
    {
        GeneralConfig config = GeneralConfig.getInstance();
        config.stopAutoSaveThread();
        try {
            WorldSaveData worldData = this.getOrCreateWorldSaveData(Constants.MOD_ID);
            Long currentTicks = config.getTotalTickCount();
            worldData.addProperty("totalTicks", parse(currentTicks) );
            this.save();
        } catch (Exception e) {
            // If the world data is not initialized, we cannot save it
            return;
        }

    }

    //write a public static method to retrieve the world properties
    public static Long getTotalTickCount() {
        ModSaveData modData = INSTANCE.getOrCreateModSavedData(Constants.MOD_ID);
        if (!modData.worldSaveData.containsKey(INSTANCE.currentWorldId))
            return -1L;

        WorldSaveData worldData = modData.getOrCreateWorldSaveData(INSTANCE.currentWorldId);
        return worldData.get("totalTicks").getAsLong();
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
