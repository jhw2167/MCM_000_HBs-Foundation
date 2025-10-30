package com.holybuckets.foundation.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.antlr.v4.runtime.misc.MultiMap;

import java.util.HashMap;
import java.util.Map;

public class StructureManager {

    private ServerLevel level;
    private Registry<Structure> structureRegistry;
    private Map<BlockPos, StructureInfo> structures;
    private MultiMap<ResourceKey<Structure>, BlockPos> structuresByType;

    private static Map<Level, StructureManager> managers = new HashMap<>();

    private StructureManager(Level level) {
        this.level = (ServerLevel) level;
        this.structures = new HashMap<>();
        this.structuresByType = new MultiMap<>();
        this.structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
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
                var resourceKey = structureRegistry.getResourceKey(structure).orElse(null);
                if(resourceKey == null) continue;
                Holder<Structure> holder = structureRegistry.getHolder(resourceKey).orElse(null);
                if(holder == null) continue;
                HolderSet<Structure> holderSet = HolderSet.direct(holder);

                var result = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, holderSet, chunk.getPos().getWorldPosition(), 100, false);
                    if(result == null) continue;
                    this.structures.put(result.getFirst(), StructureInfo.of(result, structureRegistry));
                    this.structuresByType.map(resourceKey, result.getFirst());
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

    //** Events
    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(StructureManager::onServerStart);
        reg.registerOnLevelLoad(StructureManager::onLevelLoad);
        reg.registerOnChunkLoad(StructureManager::onChunkLoad);

        reg.registerOnDataSave(StructureManager::onDataSave);
    }

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

    private static void onDataSave(DatastoreSaveEvent event) {
        for(StructureManager manager : managers.values()) {
            manager.save(event.getDataStore());
        }
    }
}
