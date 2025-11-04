package com.holybuckets.foundation.structure;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.*;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.networking.StructureInfoMessage;
import com.holybuckets.foundation.player.ManagedPlayer;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.apache.commons.logging.Log;

import java.util.*;

import static com.holybuckets.foundation.player.ManagedPlayer.registerManagedPlayerData;

public class StructureManager {

    private Level level;
    private Registry<Structure> structureRegistry; // Nullable - only available on server side
    private Map<BlockPos, StructureInfo> structures;
    private Map<ResourceLocation, Set<BlockPos>> structuresByType;


    private static Map<Level, StructureManager> managers = new HashMap<>();
    private Iterator<StructureInfo> syncIterator;

    private StructureManager(Level level) {
        this.level = level;
        this.structures = new HashMap<>();
        this.structuresByType = new HashMap<>();
        // Only initialize structure registry on server side
        if (!level.isClientSide()) {
            this.structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        }
    }

    //** GETTERS

    public static ResourceLocation toLocation(String stringStruct) {
        return new ResourceLocation(stringStruct);
    }


    public Map<BlockPos, StructureInfo> getStructures() {
        return Maps.newHashMap(structures);
    }

    public List<BlockPos> getStructurePosByType(ResourceLocation location) {
        if (!structuresByType.containsKey(location)) return List.of();
        return List.copyOf( structuresByType.get(location) );
    }

    public List<BlockPos> getStructurePosByType(Structure structure) {
        if (structureRegistry == null) return List.of();
        ResourceLocation location = structureRegistry.getKey(structure);
        if (location == null) return List.of();
        return getStructurePosByType(location);
    }

    public List<StructureInfo> getStructuresByType(ResourceLocation location) {
        if (!structuresByType.containsKey(location)) return List.of();
        return List.copyOf( structuresByType.get(location).stream().map(pos -> structures.get(pos)).toList() );
    }

    public List<StructureInfo> getStructuresByType(Structure structure) {
        if (structureRegistry == null) return List.of();
        ResourceLocation location = structureRegistry.getKey(structure);
        if (location == null) return List.of();
        return getStructuresByType(location);
    }

    public List<StructureInfo> getNearestStructures(BlockPos center, double maxDistance) {
        double maxDistSq = maxDistance * maxDistance;
        return structures.values().stream()
            .filter(info -> info.origin.distSqr(center) <= maxDistSq)
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .toList();
    }

    public List<StructureInfo> getNearestStructures(BlockPos center, int limit) {
        if(limit < 1) limit = structures.size();
        return structures.values().stream()
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .toList();
    }

    public List<StructureInfo> getNearestWhitelistedStructuresServerOnly(Set<Structure> whiteList,
                                                                         BlockPos center, int limit) {
        if (structureRegistry == null) return List.of();
        if(limit < 1) limit = structures.size();
        List<StructureInfo> allStructs = new LinkedList<>();
        for( Structure structure : whiteList ) {
            var strs = getStructuresByType(structure);
            if(strs == null) continue;
            allStructs.addAll( strs );
        }

        return allStructs.stream()
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .toList();
    }

    public List<StructureInfo> getNearestWhitelistedStructures(Set<ResourceLocation> whiteList,
                                                               BlockPos center, int limit) {
        if(limit < 1) limit = structures.size();
        List<StructureInfo> allStructs = new LinkedList<>();
        for( ResourceLocation location : whiteList ) {
            var strs = getStructuresByType(location);
            if(strs == null) continue;
            allStructs.addAll( strs );
        }

        return allStructs.stream()
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .toList();
    }


    //Returns structures NOT in the blacklist
    public List<StructureInfo> getNearestBlackListedStructuresServerOnly(Set<Structure> blackList,
                                                                         BlockPos center, int limit) {
        if (structureRegistry == null) return List.of();
        if(limit < 1) limit = structures.size();
        List<StructureInfo> allStructs = new LinkedList<>();
        for( StructureInfo info : structures.values() ) {
            Structure structure = structureRegistry.byId(info.registryId);
            if( !blackList.contains( structure ) ) {
                allStructs.add(info);
            }
        }
        return allStructs.stream()
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .toList();
    }

    //Returns structures NOT in the blacklist
    public List<StructureInfo> getNearestBlackListedStructures(Set<ResourceLocation> blackList,
                                                               BlockPos center, int limit) {
        if(limit < 1) limit = structures.size();
        List<StructureInfo> allStructs = new LinkedList<>();
        for( StructureInfo info : structures.values() ) {
            ResourceLocation structureLocation = info.getStructureLocation();
            if( structureLocation != null && !blackList.contains( structureLocation ) ) {
                allStructs.add(info);
            }
        }
        return allStructs.stream()
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .toList();
    }



    //** EVENT HANDLERS


    //Send all structures to the client to they know the position - fires once per second
    private void syncServerStructuresToClient()
    {
        if(this.syncIterator == null) {
            this.syncIterator = this.structures.values().stream().toList().iterator();
        }
        List<StructureInfo> data = new ArrayList<>();
        for(int i = 0; i < StructureInfoMessage.MAX_STRUCTURES; i++) {
            if(!this.syncIterator.hasNext()) {
                this.syncIterator = null; break;
            }
            data.add(this.syncIterator.next());
        }

        for(ServerPlayer p : HBUtil.PlayerUtil.getAllPlayers())
        {
            IManagedPlayer pData = ManagedPlayer.getManagedPlayer(p).getSubclass(PlayerStructureData.class);
            if(pData != null && (pData instanceof PlayerStructureData psData) ) {
                if (psData.getCount(level) >= this.structures.size()) continue;
                StructureInfoMessage.createAndFire(p, data);
            }
        }

    }

    private void onChunkLoad(ChunkAccess chunk)
    {
        var starts = chunk.getAllStarts().entrySet().iterator();
        while (starts.hasNext()) {
            var entry = starts.next();
            Structure structure = entry.getKey();
            StructureStart start = entry.getValue();

            if (start.isValid())
            {
                BlockPos structStartPos = start.getBoundingBox().getCenter();
                if(structures.containsKey(structStartPos)) continue;
                //LoggerBase.logInfo(null, "StructureManager", "Discovered structure " + HBUtil.BlockUtil.positionToString(structStartPos));
                if (structureRegistry == null) continue;
                ResourceLocation structureLocation = structureRegistry.getKey(structure);
                if(structureLocation == null) continue;
                ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureLocation);
                Holder<Structure> holder = structureRegistry.getHolder(structureKey).orElse(null);
                if(holder == null) continue;
                this.structures.put(structStartPos, StructureInfo.of(holder, structStartPos, structureRegistry, structureLocation));
                this.structuresByType.computeIfAbsent(structureLocation, k -> new HashSet<>()).add(structStartPos);
            }
        }


    }

    private void load(DataStore ds)
    {
        if(!GeneralConfig.getInstance().isServerSide()) return;
        JsonElement root = ds.getOrCreateLevelSaveData(Constants.MOD_ID, this.level).get("structures");
        if(root == null || root.isJsonNull()) return;

        JsonObject rootObj = root.getAsJsonObject();
        for(String key : rootObj.keySet()) {
            JsonArray arr = rootObj.getAsJsonArray(key);
            int registryId = Integer.parseInt(key);
            for(JsonElement elem : arr)
            {
                // This would need to be stored in save data or derived
                ResourceLocation structureLocation = structureRegistry.getKey( structureRegistry.byId(registryId) );
                if(structureLocation != null) {
                    StructureInfo info = StructureInfo.of(registryId, elem.getAsString(), structureRegistry, structureLocation);
                    if(structures.containsKey(info.origin)) continue;
                    this.structures.put(info.origin, info);
                    this.structuresByType.computeIfAbsent(structureLocation, k -> new HashSet<>()).add(info.origin);
                }
            }
        }
    }

    private void save(DataStore ds) {
        JsonObject root = new JsonObject();

        for(StructureInfo struct : structures.values() )
        {
            CompoundTag tag = struct.serialize();
            String key = tag.getInt("registryId")+"";
            if(!root.has(key)) { root.add(key, new JsonArray()); }
            root.getAsJsonArray(key).add(tag.getString("origin"));
        }

        ds.getOrCreateLevelSaveData(Constants.MOD_ID, this.level).addProperty("structures", root);
    }



    //** Statics
    private static StructureManager init(Level level) {
        if (!managers.containsKey(level)) {
            managers.put(level, new StructureManager(level));
        }
        return managers.get(level);
    }


    public static StructureManager get(Level level) {
        if(GeneralConfig.getInstance().isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if(!managers.containsKey(level))
            init(level);
        return managers.get(level);
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(StructureManager::onServerStart);
        reg.registerOnLevelLoad(StructureManager::onLevelLoad);
        reg.registerOnChunkLoad(StructureManager::onChunkLoad);

        reg.registerOnServerTick(TickType.ON_20_TICKS, StructureManager::on20Ticks);

        reg.registerOnSimpleMessage(STRUCTURE_MSG_ID, (e) ->
            handleSyncStructureCountsFromClient(e.getPlayer(), e.getContent())
        );

        reg.registerOnDataSave(StructureManager::onDataSave);
        PlayerStructureData.init();
    }

    public static String STRUCTURE_MSG_ID = "sync_structure_count";
    private static void handleSyncStructureCountsFromClient(Player player, String content)
    {

        IManagedPlayer pData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
        if(pData != null && (pData instanceof PlayerStructureData psData) )
        {
            Map<Level, Integer> deserialized = PlayerStructureData.deserialize(content);
            for(var entry : deserialized.entrySet()) {
                psData.put(entry.getKey(), entry.getValue());
            }
        }
    }

    //** Events
    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        StructureManager manager = StructureManager.init((Level) event.getLevel());
        if( (manager!=null) && !event.getLevel().isClientSide())
            manager.load(GeneralConfig.getInstance().getDataStore());
    }

    private static void onChunkLoad(ChunkLoadingEvent.Load event) {
        if(event.getLevel().isClientSide()) return;
        StructureManager.get((Level) event.getLevel()).onChunkLoad(event.getChunk());
    }

    private static void on20Ticks(ServerTickEvent event) {
        for(StructureManager manager : managers.values()) {
            if(manager.structures.size() > 0)
                manager.syncServerStructuresToClient();
        }
    }

    private static void onDataSave(DatastoreSaveEvent event) {
        for(StructureManager manager : managers.values()) {
            manager.save(event.getDataStore());
        }
    }


    //** CLIENT
    public static void clientInit() {
        //PlayerStructureData.init();
    }


    public static void fireSyncClientStructureCountsToServer(Player player)
    {
        //LoggerBase.logInfo(null, "050002", "fireSyncClientStructureCountsToServer");
        IManagedPlayer pData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
        if(pData != null && (pData instanceof PlayerStructureData psData) ) {
            String serializedCounts = psData.serialize();
            if(GeneralConfig.getInstance().isIntegrated())
                handleSyncStructureCountsFromClient(psData.p, serializedCounts);
            else
                SimpleStringMessage.createAndFire(player, STRUCTURE_MSG_ID, serializedCounts);
        }
    }

    public static void handleStructureInfoFromServer(Player player, StructureInfoMessage message)
    {
        if(GeneralConfig.getInstance().isIntegrated()) return;
        //LoggerBase.logInfo(null, "050004", "handleStructureInfoFromServer message: " + message.structures.size() + " structures");
        if(player == null) return;
        for(StructureInfo info : message.structures )
        {
            StructureManager sm = StructureManager.get(player.level());
            ResourceLocation structureLocation = info.getStructureLocation();
            if(structureLocation != null) {
                if(sm.structures.containsKey(info.origin)) continue;
                sm.structures.put(info.origin, info);
                sm.structuresByType.computeIfAbsent(structureLocation, k -> new HashSet<>()).add(info.origin);
                IManagedPlayer playerStructureData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
                if(playerStructureData != null && (playerStructureData instanceof PlayerStructureData psData) ) {
                    psData.increment( (Level) player.level() );
                }
            }

        }


    }

    public static void onConnectedToServer(Player player) {

        if(GeneralConfig.getInstance().isServerSide()) return;
        managers.clear();
        IManagedPlayer pData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
        if(pData != null && (pData instanceof PlayerStructureData psData) ) {
            psData.clearAll();
        }
    }

    //** Classes

    static class PlayerStructureData implements IManagedPlayer {

        Map<Level, Integer> syncedStructureCounts;
        Player p;

        static void init() {
            registerManagedPlayerData(PlayerStructureData.class, () -> new PlayerStructureData(null));
        }
        public PlayerStructureData(Player player) {
            this.syncedStructureCounts = new HashMap<>();
            setPlayer(player);
        }

        @Override
        public boolean isInit(String subclass) { return true; }

        @Override
        public IManagedPlayer getStaticInstance(Player player, String id) {
            return null;
        }

        @Override
        public void handlePlayerJoin(Player player) {
            StructureManager.onConnectedToServer(player);
        }

        public int getCount(Level level) {
            return this.syncedStructureCounts.getOrDefault(level, 0);
        }

        public void put(Level level, int count) {
            this.syncedStructureCounts.computeIfAbsent(level, lvl -> 0);
            this.syncedStructureCounts.put(level, count);
        }

        public void increment(Level level) {
            this.syncedStructureCounts.computeIfAbsent(level, lvl -> 0);
            this.syncedStructureCounts.put(level, this.syncedStructureCounts.get(level) + 1);
        }

        public void clear(Level level) {
            this.syncedStructureCounts.put(level, 0);
        }

        public void clearAll() {
            this.syncedStructureCounts.clear();
        }

        public String serialize() {

            if(this.p.level() == null) return "{}";
            else if( !syncedStructureCounts.containsKey(p.level())) {
                put(p.level(), 0);
            }

            JsonObject structCounts = new JsonObject();
            for(var entry : this.syncedStructureCounts.entrySet()) {
                structCounts.addProperty( HBUtil.LevelUtil.toLevelId(entry.getKey()) , entry.getValue() );
            }
            return structCounts.toString();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString ("syncedStructuresCount", serialize() );
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            String json = nbt.getString("syncedStructuresCount");
            for(var entry : deserialize(json).entrySet()) {
                this.syncedStructureCounts.put(entry.getKey(), entry.getValue());
            }
        }

        public static Map<Level, Integer> deserialize(String json)
        {
            Map<Level, Integer> data = Maps.newHashMap();
            if(json == null || json.isEmpty()) return data;
            for(var entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
                Level level = HBUtil.LevelUtil.toLevel(null, entry.getKey());
                if(level == null) continue;
                int count = entry.getValue().getAsInt();
                data.put(level, count);
            }
            return data;
        }

        @Override
        public void setId(String id) {}

        @Override
        public void setPlayer(Player player) { this.p = player; }


    }
}