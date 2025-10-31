package com.holybuckets.foundation.structure;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.networking.StructureInfoMessage;
import com.holybuckets.foundation.player.ManagedPlayer;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.client.Minecraft;
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
import org.antlr.v4.runtime.misc.MultiMap;

import java.util.*;

import static com.holybuckets.foundation.player.ManagedPlayer.registerManagedPlayerData;

public class StructureManager {

    private ServerLevel level;
    private Registry<Structure> structureRegistry;
    private Map<BlockPos, StructureInfo> structures;
    private MultiMap<ResourceKey<Structure>, BlockPos> structuresByType;


    private static Map<Level, StructureManager> managers = new HashMap<>();
    private Iterator<StructureInfo> syncIterator;

    private StructureManager(Level level) {
        this.level = (ServerLevel) level;
        this.structures = new HashMap<>();
        this.structuresByType = new MultiMap<>();
        this.structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
    }

    //** GETTERS
    public static ResourceKey<Structure> toKey(ResourceLocation loc) {
        return ResourceKey.create(Registries.STRUCTURE, loc);
    }

    public static ResourceKey<Structure> toKey(Structure structure) {
        ResourceLocation loc = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type());
        return toKey(loc);
    }

    public static ResourceKey<Structure> toKey(String stringStruct) {
            return toKey(new ResourceLocation(stringStruct));
    }


    public Map<BlockPos, StructureInfo> getStructures() {
        return Maps.newHashMap(structures);
    }

    public List<BlockPos> getStructurePosByType(ResourceKey<Structure> key) {
        return List.copyOf( structuresByType.get(key) );
    }

        public List<BlockPos> getStructurePosByType(ResourceLocation location) {
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, location);
            return getStructurePosByType(key);
        }

        public List<BlockPos> getStructurePosByType(Structure structure) {
            ResourceKey<Structure> key = structureRegistry.getResourceKey(structure).orElse(null);
            if (key == null) return List.of();
            return getStructurePosByType(key);
        }

    public List<StructureInfo> getStructuresByType(ResourceKey<Structure> key) {
        return List.copyOf( structuresByType.get(key).stream().map(pos -> structures.get(pos)).toList() );
    }

        public List<StructureInfo> getStructuresByType(ResourceLocation location) {
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, location);
            return getStructuresByType(key);
        }

        public List<StructureInfo> getStructuresByType(Structure structure) {
            ResourceKey<Structure> key = structureRegistry.getResourceKey(structure).orElse(null);
            if (key == null) return List.of();
            return getStructuresByType(key);
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

    public List<StructureInfo> getNearestWhitelistedStructures(Set<ResourceKey<Structure>> whiteList,
                                                               BlockPos center, int limit) {
        if(limit < 1) limit = structures.size();
        return structuresByType.entrySet().stream()
                .filter(key ->  whiteList.contains(key))
                .flatMap(entry -> entry.getValue().stream().map(pos -> structures.get(pos)) )
                .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
                .limit(limit)
                .map(info -> structures.get(info))
                .toList();
    }


    //Returns structures NOT in the blacklist
    public List<StructureInfo> getNearestBlackListedStructures(Set<ResourceKey<Structure>> blackList,
                                                               BlockPos center, int limit) {
        if(limit < 1) limit = structures.size();
        return structuresByType.entrySet().stream()
            .filter(key ->  !blackList.contains(key))
            .flatMap(entry -> entry.getValue().stream().map(pos -> structures.get(pos)) )
            .sorted(Comparator.comparingDouble(a -> a.origin.distSqr(center)))
            .limit(limit)
            .map(info -> structures.get(info))
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
                StructureInfoMessage.createAndFire(null, data);
            }
        }

    }

    private void syncClientStructureCountsToServer() {
        IManagedPlayer pData = ManagedPlayer.getManagedPlayer(Minecraft.getInstance().player).getSubclass(PlayerStructureData.class);
        if(pData != null && (pData instanceof PlayerStructureData psData) ) {
            SimpleStringMessage.create
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
                //LoggerBase.logInfo(null, "StructureManager", "Discovered structure " + HBUtil.BlockUtil.positionToString(structStartPos));
                var resourceKey = structureRegistry.getResourceKey(structure).orElse(null);
                if(resourceKey == null) continue;
                Holder<Structure> holder = structureRegistry.getHolder(resourceKey).orElse(null);
                if(holder == null) continue;

                    this.structures.put(structStartPos, StructureInfo.of(holder, structStartPos, structureRegistry));
                    this.structuresByType.map(resourceKey, structStartPos);
            }
        }


    }

    private void load(DataStore ds)
    {
        JsonElement root = ds.getOrCreateLevelSaveData(Constants.MOD_ID, this.level).get("structures");
        if(root == null || root.isJsonNull()) return;

        JsonObject rootObj = root.getAsJsonObject();
        for(String key : rootObj.keySet()) {
            JsonArray arr = rootObj.getAsJsonArray(key);
            int registryId = Integer.parseInt(key);
            for(JsonElement elem : arr)
            {
                StructureInfo info = StructureInfo.of(registryId, elem.getAsString(), structureRegistry);
                var resourceKey = structureRegistry.getResourceKey(structureRegistry.byId(registryId)).orElse(null);
                if(resourceKey != null) {
                    this.structures.put(info.origin, info);
                    this.structuresByType.map(resourceKey, info.origin);
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

    public static StructureManager get(Level level) {
        return managers.computeIfAbsent(level, lvl -> new StructureManager(level));
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(StructureManager::onServerStart);
        reg.registerOnLevelLoad(StructureManager::onLevelLoad);
        reg.registerOnChunkLoad(StructureManager::onChunkLoad);

        reg.registerOnServerTick(TickType.ON_20_TICKS, StructureManager::on20Ticks);


        reg.registerOnSimpleMessage( "sync_structure_count", StructureManager::handleSyncStructureCountsFromClient);

        reg.registerOnDataSave(StructureManager::onDataSave);
        PlayerStructureData.init();
    }



    //** Events
    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if(event.getLevel().isClientSide()) return;
        StructureManager.get((Level) event.getLevel()).load(GeneralConfig.getInstance().getDataStore());
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

    //run on client
    public static void handleStructureInfoFromServer(Player player, StructureInfoMessage message) {
        if(player == null) return;
        for(StructureInfo info : message.structures) {
            StructureManager sm = StructureManager.get(player.level());
            sm.structures.put(info.origin, info);
            var resourceKey = sm.structureRegistry.getResourceKey(sm.structureRegistry.byId(info.registryId)).orElse(null);
            if(resourceKey != null) {
                sm.structuresByType.map(resourceKey, info.origin);
                IManagedPlayer playerStructureData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
                if(playerStructureData != null && (playerStructureData instanceof PlayerStructureData psData) ) {
                    psData.increment( (Level) player.level() );
                }
            }
        }
    }

    //run on server
    private static void handleSyncStructureCountsFromClient(SimpleMessageEvent simpleMessageEvent) {
        Player player = simpleMessageEvent.getPlayer();
        String content = simpleMessageEvent.getMessage().content;

        IManagedPlayer pData = ManagedPlayer.getManagedPlayer(player).getSubclass(PlayerStructureData.class);
        if(pData != null && (pData instanceof PlayerStructureData psData) )
        {
            Map<Level, Integer> deserialized = PlayerStructureData.deserialize(content);
            for(var entry : deserialized.entrySet()) {
                psData.put(entry.getKey(), entry.getValue());
            }
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
        public void handlePlayerJoin(Player player) {}

        public int getCount(ServerLevel level) {
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

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            JsonObject structCounts = new JsonObject();
            for(var entry : this.syncedStructureCounts.entrySet()) {
                structCounts.addProperty( HBUtil.LevelUtil.toLevelId(entry.getKey()) , entry.getValue() );
            }
            tag.putString ("syncedStructuresCount", structCounts.toString() );
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
                boolean isClient = GeneralConfig.getInstance().isClientSide();
                Level level = (isClient) ? HBUtil.LevelUtil.toClientLevel(entry.getKey()) :
                    HBUtil.LevelUtil.toServerLevel(entry.getKey());
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
