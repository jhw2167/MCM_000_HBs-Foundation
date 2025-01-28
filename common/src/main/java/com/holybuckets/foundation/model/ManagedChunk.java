package com.holybuckets.foundation.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;

import com.holybuckets.foundation.networking.BlockStateUpdatesMessage;
import com.holybuckets.foundation.networking.BlockStateUpdatesMessageHandler;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    //public static final String NBT_KEY_HEADER = "managedChunk";

    static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();
    static final Map<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, Map<String, ManagedChunk>> LOADED_CHUNKS = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, Set<String>> INITIALIZED_CHUNKS = new ConcurrentHashMap<>();

    private String id;
    private ChunkPos pos;
    private LevelAccessor level;
    private int tickWritten;
    private int tickLoaded;
    private boolean isLoaded;
    private final HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> managedChunkData = new HashMap<>();



    /** CONSTRUCTORS **/
    private ManagedChunk() {
        super();
    }

    public ManagedChunk( CompoundTag tag ) {
        this();
        this.deserializeNBT(tag);
        LOADED_CHUNKS.get(this.level).put(this.id, this);
    }

    public ManagedChunk(LevelAccessor level, ChunkPos pos )
    {
        this();
        this.id = HBUtil.ChunkUtil.getId(pos);
        this.pos = pos;
        this.level = level;

        this.tickLoaded = GENERAL_CONFIG.getServer().getTickCount();
        this.initSubclassesFromMemory(level, id);

        LOADED_CHUNKS.get(this.level).put(this.id, this);
        INITIALIZED_CHUNKS.get(this.level).add(this.id);
    }


    /** GETTERS and SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject);
    }

    //set chunk to null on chunkUnload
    public LevelChunk getChunk(boolean forceLoad) {
        return ManagedChunkUtilityAccessor.getChunk(this.level, this.id, forceLoad);
    }

    public LevelAccessor getLevel() {
        return this.level;
    }

    public String getId() {
        return this.id;
    }



    /**
     * Set a managed chunk data subclass
     * @param classObject The class of the managed chunk data
     * @param data The managed chunk data instance
     * @return true if set successfully
     */
    public Boolean setSubclass(Class<? extends IMangedChunkData> classObject, IMangedChunkData data)
    {
        if (classObject == null || data == null) {
            return false;
        }
        managedChunkData.put(classObject, data);
        return true;
    }


    /**
     * Initialize subclasses using getStaticInstance method from each subclass. Which
     * allows each subclass to initialize itself from an existing datastructure owned by
     * a class unknown to Managed Chunk. useSerialize is a boolean, that if set to true
     * the subclass will be skipped since more correct data is from the serialized data.
     * @param level
     * @param chunkId
     */
    private void initSubclassesFromMemory(LevelAccessor level, String chunkId)
    {
        for(Map.Entry<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> data : MANAGED_SUBCLASSES.entrySet() ) {
            setSubclass( data.getKey() , data.getValue().get().getStaticInstance(level, chunkId) );
        }

    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId
    {
        //Loop over all subclasses and deserialize if matching chunk not found in RAM
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> data : MANAGED_SUBCLASSES.entrySet() )
        {
            IMangedChunkData sub = data.getValue().get();
            try {
                sub.deserializeNBT(tag.getCompound(sub.getClass().getName()));
                setSubclass(sub.getClass(), sub);
            } catch (Exception e) {
                errors.put(sub.getClass().getName(), e.getMessage());
            }

        }

        if(!errors.isEmpty())
        {
            //Add all errors in list to error message
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            throw new InvalidId(error.toString());
        }
    }


    /** OVERRIDES **/
    private void init(CompoundTag tag) throws InvalidId
    {
        //print tag as string, info
        this.id = tag.getString("id");

        this.level = HBUtil.LevelUtil.toLevel( HBUtil.LevelUtil.LevelNameSpace.SERVER, tag.getString("level"));
        this.tickWritten = tag.getInt("tickWritten");

        /** If tickWritten is < tickLoaded, then this data
         * was written previously and removed from memory. Replace the dummy
         * with serialized data.
         */
        //if( this.tickWritten < this.tickLoaded )
        if( this.isLoaded )
        {
            LoggerBase.logInfo(null, "003007", "Init from nbt id: " + this.id);
            this.initSubclassesFromMemory(level, id);
        }
        else
        {
            LoggerBase.logInfo(null, "003006", "Init from memory id: " + this.id);
            this.initSubclassesFromNbt(tag);
        }

        this.tickLoaded = GENERAL_CONFIG.getServer().getTickCount();

    }

    /**
     * Check if all subclasses are not null and initialized successfully
     * @return boolean
     */
    @Override
    public boolean isInit(String subClass) {
        for(IMangedChunkData data : managedChunkData.values())
        {
            if( !data.getClass().getName().equals(subClass) )
                continue;

            if( !data.isInit(subClass) )
                return false;
            else
                return true;
        }
        return false;
    }

    /**
     * Override, dummy method
     * @param level
     * @param id
     * @return
     */
    @Override
    public IMangedChunkData getStaticInstance(LevelAccessor level, String id) {
        return ManagedChunkUtilityAccessor.getManagedChunk(level, id);
    }


    @Override
    public void handleChunkLoaded(ChunkLoadingEvent.Load event)
    {
        LoggerBase.logInfo(null, "003005", "Loading ManagedChunk with id: " + this.id);

        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkLoaded(event);
        }
        this.isLoaded = true;
    }

    @Override
    public void handleChunkUnloaded(ChunkLoadingEvent.Unload event)
    {
        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkUnloaded(event);
        }
        this.isLoaded = false;
    }




    /** STATIC UTILITY METHODS **/

    public static void init( EventRegistrar reg )
    {
        reg.registerOnLevelLoad(ManagedChunk::onWorldLoad);
        reg.registerOnLevelUnload(ManagedChunk::onWorldUnload);

        reg.registerOnChunkLoad(ManagedChunk::onChunkLoad);
        reg.registerOnChunkUnload(ManagedChunk::onChunkUnload);

        ManagedChunkBlockUpdates.init(reg);
    }

    public static void onWorldLoad( final LevelLoadingEvent.Load event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;
        DataStore ds = GeneralConfig.getInstance().getDataStore();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( Constants.MOD_ID, level);


        if(LOADED_CHUNKS.get(level) == null)
        {
            LOADED_CHUNKS.put(level, new ConcurrentHashMap<>());
            INITIALIZED_CHUNKS.put(level,  new ConcurrentSet<>());
        }

        JsonElement chunksIds = levelData.get("chunkIds");
        if( chunksIds == null )
        {
            String[] ids = new String[0];
            levelData.addProperty("chunkIds", HBUtil.FileIO.arrayToJson(ids));
            chunksIds = levelData.get("chunkIds");
        }

        Set<String> initChunks = INITIALIZED_CHUNKS.get(level);
        chunksIds.getAsJsonArray().forEach( chunkId -> {
            initChunks.add(chunkId.getAsString());
        });

        EventRegistrar.getInstance().registerOnDataSave(() -> save(level), true);
    }

    public static void onWorldUnload( final LevelLoadingEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        save(level);
    }

    public static void onChunkLoad( final ChunkLoadingEvent.Load event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        String chunkId = HBUtil.ChunkUtil.getId(event.getChunk());
        ManagedChunk loadedChunk = LOADED_CHUNKS.get(level).get(chunkId);

        if(loadedChunk == null)
        {
            LoggerBase.logDebug(null, "003008.1", "Creating new managed Chunk: " + chunkId);
            loadedChunk = new ManagedChunk(level, event.getChunkPos());
        }

        loadedChunk.handleChunkLoaded(event);
    }

    public static void onChunkUnload( final ChunkLoadingEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        ChunkAccess chunk = event.getChunk();
        String id = HBUtil.ChunkUtil.getId(chunk);
        ManagedChunk c = LOADED_CHUNKS.get(level).remove(id);
        if( c == null )
            return;
        c.handleChunkUnloaded(event);
    }

    /**
     * Update the blocks of a chunk. Calls updateChunkBlockStates with the default block state of the block.
     * @param level
     * @param updates
     * @return true if all updates were successful, false otherwise
     */
    public static boolean updateChunkBlocks(LevelAccessor level, Map<BlockState, List<BlockPos>> updates) {
        return ManagedChunkBlockUpdates.updateChunkBlocks(level, updates);
    }

    /**
     * Update the blocks of a chunk. Calls updateChunkBlockStates with the default block state of the block.
     * @param level
     * @param updates
     * @return true if all updates were successful, false otherwise
     */
    public static boolean updateChunkBlocks(LevelAccessor level, List<Pair<Block, BlockPos>> updates) {
        return ManagedChunkBlockUpdates.updateChunkBlocks(level, updates);
    }


    /**
     * Update the block states of in world on a Client or ServerLevel. These results are added to a queue and
     * processed in the next tick where setBlock can run on the main thread.
     * @param level
     * @param updates
     * @return true if successful, false if some element was null
     */
    public static boolean updateChunkBlockStates(final LevelAccessor level, List<Pair<BlockState, BlockPos>> updates) {
        return ManagedChunkBlockUpdates.updateChunkBlockStatesAndCast(level, updates);
    }


    public static void registerManagedChunkData(Class<? extends IMangedChunkData> classObject, Supplier<IMangedChunkData> data)
    {
        MANAGED_SUBCLASSES.put(classObject, data);
    }

    private static void save( LevelAccessor level )
    {
        //Write out initialzed chunks to levelSaveData
        DataStore ds = GENERAL_CONFIG.getDataStore();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( Constants.MOD_ID, level);

        Set<String> initChunks = INITIALIZED_CHUNKS.get(level);
        if(initChunks == null) return;

        String[] chunkIds = initChunks.toArray(new String[0]);
        levelData.addProperty("chunkIds", HBUtil.FileIO.arrayToJson(chunkIds) );

    }



    /** SERIALIZERS **/

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag details = new CompoundTag();

        if( this.id == null || this.level == null ) {
            LoggerBase.logError(null, "003004", "ManagedChunk not initialized with id or level and cannot be serialized");
            return details;
        }

        int count = 0;
        try {

            details.putString("id", this.id); count++;
            details.putString("level", HBUtil.LevelUtil.toLevelId(this.level)); count++;
            this.tickWritten = GENERAL_CONFIG.getServer().getTickCount(); count++;
            details.putInt("tickWritten", this.tickWritten); count++;

            for(IMangedChunkData data : managedChunkData.values())
            {
                if( data == null )
                    continue;
                //TO DO: Thread this operation and lock until data object is done with current operation
                //to ensure write is most recent info
                details.put(data.getClass().getName(), data.serializeNBT());
                count++;
            }


        }
        catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error serializing ManagedChunk with id: ");
            error.append(this.id);
            error.append("\nError: ");
            error.append(e.getClass());
            error.append(" - ");
            error.append(e.getMessage());
            error.append("\nCount: ");
            error.append(count);

            LoggerBase.logError(null, "003020", error.toString());
        }

        LoggerBase.logDebug( null,"003002", "Serializing ManagedChunk with data: " + details);
        return details;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null || tag.isEmpty()) {
            return;
        }

        LoggerBase.logInfo(null, "003001", "Deserializing ManagedChunk with id: " + this.id);

        //Deserialize subclasses
        try {
            this.init(tag);
        } catch (InvalidId e) {
            LoggerBase.logError(null, "003021", "Error initializing ManagedChunk with id: " + id);
        }


    }


}
//END CLASS