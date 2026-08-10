package com.holybuckets.foundation.model;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;

import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    //public static final String NBT_KEY_HEADER = "managedChunk";

    static GeneralConfig GENERAL_CONFIG;
    static final Map<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, Map<String, ManagedChunk>> LOADED_CHUNKS = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, Map<ChunkPos, String>> LOADED_CHUNKPOS = new ConcurrentHashMap<>();
    static final Map<LevelAccessor,ConcurrentSet<ManagedChunk>> CHUNK_CACHE = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, Set<String>> INITIALIZED_CHUNKS = new ConcurrentHashMap<>();
    static final Map<LevelAccessor, ConcurrentSet<Long>> INITIALIZED_LONG_CHUNKS = new ConcurrentHashMap<>();

    private String id;
    private LevelAccessor level;
    private ChunkPos pos;
    ChunkAccess levelChunk;
    private long tickWritten;
    private long tickLoaded;
    private boolean isLoaded;
    private final HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> managedChunkData = new HashMap<>();
    public ManagedChunkUtility util;



    /** CONSTRUCTORS **/
    private ManagedChunk() {
        super();
        this.util = null;
    }

    public ManagedChunk( CompoundTag tag ) {
        this();
        this.deserializeNBT(tag);
        this.pos = HBUtil.ChunkUtil.getChunkPos(this.id);
        this.util = ManagedChunkUtility.getInstance(this.level);
        LOADED_CHUNKS.get(this.level).put(this.id, this);
        if(pos != null)
            LOADED_CHUNKPOS.get(this.level).put(pos, this.id);
    }

    public ManagedChunk(LevelAccessor level, ChunkPos pos )
    {
        this();
        this.id = HBUtil.ChunkUtil.getId(pos);
        this.pos = pos;
        this.level = level;
        this.util = ManagedChunkUtility.getInstance(level);

        if(!this.level.isClientSide())
        {
            this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
            this.initSubclasses(level, id, null);
        }

        LOADED_CHUNKS.putIfAbsent(this.level, new ConcurrentHashMap<>());
        LOADED_CHUNKPOS.putIfAbsent(this.level, new ConcurrentHashMap<>());

        INITIALIZED_CHUNKS.putIfAbsent(this.level, new HashSet<>());
        INITIALIZED_LONG_CHUNKS.putIfAbsent(this.level, new ConcurrentSet<>());

        CHUNK_CACHE.putIfAbsent(this.level, new ConcurrentSet<>());
        LOADED_CHUNKS.get(this.level).put(this.id, this);
        LOADED_CHUNKPOS.get(this.level).put(pos, this.id);

        INITIALIZED_CHUNKS.get(this.level).add(this.id);
        INITIALIZED_LONG_CHUNKS.get(this.level).add(HBUtil.ChunkUtil.getChunkPos1DMap(pos.x(), pos.z()));
    }


    /** GETTERS and SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject);
    }

    public String getId() {
        return this.id;
    }

    public LevelAccessor getLevel() {
        return this.level;
    }

    public ChunkPos getChunkPos() {
        if(this.pos == null)
            this.pos = HBUtil.ChunkUtil.getChunkPos(this.id);
        return this.pos;
    }

    public BlockPos getWorldPos() { return HBUtil.ChunkUtil.getWorldPos(this.id); }

    public LevelChunk getCachedLevelChunk() {
        if(this.levelChunk instanceof LevelChunk)
            return (LevelChunk) this.levelChunk;
        return null;
    }

    /**
     * DO NOT ATTEMPT THIS FROM MAIN THREAD LIKE A TICK OR A COMMAND, will crash world
     * @return LevelChunk
     */
    public LevelChunk getLevelChunk() {
        if(this.levelChunk instanceof LevelChunk)
            return (LevelChunk) this.levelChunk;
        else if( util.isChunkFullyLoaded(id) ) {
            if(this.level.isClientSide())
                return (LevelChunk) this.level.getChunk(this.pos.x(), this.pos.z());

            ChunkAccess c = this.level.getChunk(this.pos.x(), this.pos.z());
            if(c instanceof LevelChunk) this.levelChunk = c;
            return (LevelChunk) this.levelChunk;
        }
        return  null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLevel(LevelAccessor level) {
        this.level = level;
    }

    void releaseLevelChunk() { this.levelChunk = null; }


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


    private void initSubclasses(LevelAccessor level, String chunkId, @Nullable CompoundTag tag)
    {
        //Loop over all subclasses and deserialize if matching chunk not found in RAM
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> data : MANAGED_SUBCLASSES.entrySet() )
        {
            IMangedChunkData subData = data.getValue().get();
            subData.setLevel(level);
            subData.setId(chunkId);
            String className = data.getKey().getName();
            if(tag == null || tag.isEmpty() || !tag.contains(className)) {
                //no deserialization
            }
            else
            {
                try {
                    CompoundTag subTag = tag.getCompoundOrEmpty(className);
                    subData.deserializeNBT(subTag);
                } catch (Exception e) {
                    errors.put(data.getKey().getName(), e.getMessage());
                    continue;
                }
            }

            IMangedChunkData resolved = subData.resolveSubData(level, chunkId, subData);
            if( resolved != null ) {
                managedChunkData.put(data.getKey(), resolved);
            }

        }

        if(!errors.isEmpty())
        {
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            LoggerBase.logError(null, "003021",
            "Error initializing subclasses for chunk with id: " + chunkId + "\nErrors: \n" + error);
        }
    }

    /** OVERRIDES **/
    private void init(CompoundTag tag)
    {
        //print tag as string, info
        this.id = tag.getString("id").orElse(null);

        this.level = HBUtil.LevelUtil.toLevel( HBUtil.LevelUtil.LevelNameSpace.SERVER, tag.getStringOr("level", ""));
        this.tickWritten = tag.getLongOr("tickWritten", 0L);

        /** If tickWritten is < tickLoaded, then this data
         * was written previously and removed from memory. Replace the dummy
         * with serialized data.
         */
        this.initSubclasses(level, id, tag);
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();

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


    @Nullable
    @Override
    public IMangedChunkData resolveSubData(LevelAccessor level, String id, @Nullable IMangedChunkData serialized) {
        return ManagedChunkUtility.getInstance(level).getManagedChunk(id);
    }


    @Override
    public void handleChunkLoaded(ChunkLoadingEvent.Load event)
    {
        this.isLoaded = true;
        if(this.level.isClientSide()) return;
        this.levelChunk = event.getChunk();
        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkLoaded(event);
        }
    }

    @Override
    public void handleChunkUnloaded(ChunkLoadingEvent.Unload event)
    {
        this.isLoaded = false;
        if(this.level.isClientSide()) return;
        this.levelChunk = null;
        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkUnloaded(event);
        }
    }




    /** STATIC UTILITY METHODS **/

    public static void init( EventRegistrar reg )
    {
        ManagedChunkUtility.init(reg);
        ManagedChunkEvents.init(reg);
        ManagedChunkBlockUpdates.init(reg);
        GENERAL_CONFIG = GeneralConfig.getInstance();
    }

    /**
     * Update the blocks of a chunk. Calls updateChunkBlockStates with the default block state of the block.
     * @param level
     * @param updates
     * @return true if all updates were successful, false otherwise
     */
    public static boolean updateChunkBlockStates(LevelAccessor level, BlockState state, Collection<BlockPos> updates) {
        Map<BlockState, List<BlockPos>> blockStateMap = Map.of(state, new ArrayList<>(updates));
        return updateChunkBlockStates(level, blockStateMap);
    }


    /**
     * Update the blocks of a chunk. Calls updateChunkBlockStates with the default block state of the block.
     * @param level
     * @param updates
     * @return true if all updates were successful, false otherwise
     */
    public static boolean updateChunkBlockStates(LevelAccessor level, Map<BlockState, List<BlockPos>> updates) {
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
     * processed in the next tick where setBlock can run on the main thread. If ANY block position
     * in the update is not in a location loaded by the server, no blocks are updated and false is returned.
     * @param level
     * @param updates
     * @return true if successful, false if some element was null
     */
    public static boolean updateChunkBlockStates(final LevelAccessor level, List<Pair<BlockState, BlockPos>> updates) {
        return ManagedChunkBlockUpdates.updateChunkBlockStates(level, updates);
    }

    /**
     * Check if the update has succeeded and removes it.
     * @param level - LevelAccessor
     * @param update - Pair object with the same hashcode as requested update
     * @return true if the update has succeeded
     */
    public static boolean checkUpdateBlockStateSucceeded(LevelAccessor level, Pair<BlockState, BlockPos> update) {
        return ManagedChunkBlockUpdates.checkUpdateBlockStateSucceeded(level, update);
    }

    public static void registerManagedChunkData(Class<? extends IMangedChunkData> classObject, Supplier<IMangedChunkData> data)
    {
        MANAGED_SUBCLASSES.put(classObject, data);
    }

    static void save(DatastoreSaveEvent event, Level level)
    {
        //Write out initialzed chunks to levelSaveData
        DataStore ds = event.getDataStore();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( Constants.MOD_ID, level);

        Set<String> initChunks = INITIALIZED_CHUNKS.get(level);
        if(initChunks == null) return;

        String[] chunkIds = initChunks.toArray(new String[0]);
        levelData.addProperty("initializedChunkIds", HBUtil.FileIO.arrayToJson(chunkIds) );

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
            this.tickWritten = GENERAL_CONFIG.getTotalTickCount(); count++;
            details.putLong("tickWritten", this.tickWritten); count++;

            for(IMangedChunkData data : managedChunkData.values())
            {
                if( data == null ) continue;
                CompoundTag serialData = data.serializeNBT();
                if( serialData == null || serialData.isEmpty()) continue;
                details.put(data.getClass().getName(), serialData);
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

        //LoggerBase.logDebug( null,"003002", "Serializing ManagedChunk with data: " + details);
        return details;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null || tag.isEmpty())return;
        this.init(tag);
    }

    /*
    @Override
    public int hashCode() {
        return HBUtil.ChunkUtil.getChunkPos1DMap(this.getChunkPos());
    }
    */


}
//END CLASS
