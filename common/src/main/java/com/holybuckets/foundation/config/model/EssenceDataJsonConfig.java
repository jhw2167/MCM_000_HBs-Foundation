package com.holybuckets.foundation.config.model;

import com.google.gson.*;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import java.util.*;

public class EssenceDataJsonConfig implements IStringSerializable {

    public static final String DEF_ESSENCE_FILE_CONFIG_PATH = "config/HBFoundationEssenceConfig.json";
    public static final EssenceDataJsonConfig DEFAULT_CONFIG = buildDefaultConfig();

    private Map<String, EssenceConfig> essenceMap;

    public static class EssenceConfig {
        public String id;
        public Set<String> biomes;
        public Set<String> entities;
        public Set<String> structures;
        public Set<String> levelIds;
        public Set<String> items;

        public Set<String> all;

        public EssenceConfig(String id, Set<String> biomes, Set<String> entities, Set<String> structures, Set<String> dimensions, Set<String> items) {
            this.id = id;
            this.biomes = biomes;
            this.entities = entities;
            this.structures = structures;
            this.levelIds = dimensions;
            this.items = items;
            this.all = new HashSet<>();

            if (biomes != null) this.all.addAll(biomes);
            if (entities != null) this.all.addAll(entities);
            if (dimensions != null) this.all.addAll(dimensions);
            if (levelIds != null) this.all.addAll(levelIds);
            if (items != null) this.all.addAll(items);
        }

        public EssenceConfig(String id, JsonObject obj) {
            this(id, parseResourceLocationArray(obj, "biomes")
                , parseResourceLocationArray(obj, "entities")
                , parseResourceLocationArray(obj, "structures")
                , parseResourceLocationArray(obj, "dimensions")
                , parseResourceLocationArray(obj, "items"));
        }
    }

    public EssenceDataJsonConfig(List<EssenceConfig> configs) {
        this.essenceMap = new HashMap<>();
        configs.forEach(config -> essenceMap.put(config.id, config));
    }

    public EssenceDataJsonConfig(String jsonString) {
        this(List.of());
        deserialize(jsonString);
    }

    @Override
    public String serialize() {
        return serializeConfigs(essenceMap);
    }

    @Override
    public void deserialize(String jsonString) {
        Gson gson = new Gson();
        JsonObject rootObject = gson.fromJson(jsonString, JsonObject.class);

        if (rootObject != null && rootObject.has("essenceConfigs")) {
            JsonArray essenceArray = rootObject.getAsJsonArray("essenceConfigs");

            for (JsonElement element : essenceArray) {
                if (element.isJsonObject()) {
                    JsonObject essenceObj = element.getAsJsonObject();
                    String id = essenceObj.get("id").getAsString();
                    essenceMap.put(id, new EssenceConfig(id, essenceObj));
                }
            }
        }
    }

    private static Set<String> parseResourceLocationArray(JsonObject obj, String fieldName) {
        Set<String> set = new HashSet<>();
        if (obj.has(fieldName)) {
            JsonArray array = obj.getAsJsonArray(fieldName);
            for (JsonElement element : array) {
                if (element.isJsonPrimitive())
                    set.add(element.getAsString());
            }
        }

        return set.isEmpty() ? null : set;
    }

    public Set<String> getAllEssenceIds() {
        return essenceMap.keySet();
    }

    public EssenceConfig getConfig(String id) {
        return essenceMap.get(id);
    }


    private static EssenceDataJsonConfig buildDefaultConfig()
    {
        List<EssenceConfig> configs = new ArrayList<>();


        configs.add(new EssenceConfig("undead",
            null, Set.of("minecraft:skeleton"),
            null, null,
            Set.of("minecraft:bone")));

        configs.add(new EssenceConfig("rotten",
            null, Set.of("minecraft:zombie"),
            null, null,
            Set.of("minecraft:rotten_flesh")));

        // Earthen Essence
        configs.add(new EssenceConfig("earthen",
            Set.of("minecraft:plains", "minecraft:meadow", "minecraft:forest", "minecraft:birch_forest"),
            null,
            null, null,
            Set.of("minecraft:dirt")));

        // Sunflower Essence
        configs.add(new EssenceConfig("sunflower",
            Set.of("minecraft:sunflower_plains"),
            null,
            null, null,
            Set.of("minecraft:sunflower")));

        // Savannah Essence
        configs.add(new EssenceConfig("savannah",
            Set.of("minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"),
            null,
            null, null,
            Set.of("minecraft:acacia_log")));

        // Pastoral Essence
        configs.add(new EssenceConfig("pastoral",
            Set.of("minecraft:plains", "minecraft:meadow"),
            null,
            null, null,
            Set.of("minecraft:wheat")));

        // Birch Essence
        configs.add(new EssenceConfig("birch",
            Set.of("minecraft:birch_forest", "minecraft:old_growth_birch_forest"),
            null,
            null, null,
            Set.of("minecraft:birch_sapling")));

        // Wildflower Essence
        configs.add(new EssenceConfig("wildflower",
            Set.of("minecraft:flower_forest"),
            null,
            null, null,
            Set.of("minecraft:poppy")));

        // Sylvan Essence
        configs.add(new EssenceConfig("sylvan",
            Set.of("minecraft:forest", "minecraft:windswept_forest"),
            null,
            null, null,
            Set.of("minecraft:oak_sapling")));

        // Dark Essence
        configs.add(new EssenceConfig("dark",
            Set.of("minecraft:dark_forest"),
            null,
            null, null,
            Set.of("minecraft:dark_oak_sapling")));

        // Cherry Blossom Essence
        configs.add(new EssenceConfig("cherry_blossom",
            Set.of("minecraft:cherry_grove"),
            null,
            null, null,
            Set.of("minecraft:pink_petals")));

        // Riverine Essence
        configs.add(new EssenceConfig("riverine",
            Set.of("minecraft:river", "minecraft:frozen_river"),
            null,
            null, null,
            Set.of("minecraft:clay_ball")));

        // Coastal Essence
        configs.add(new EssenceConfig("coastal",
            Set.of("minecraft:beach", "minecraft:stony_shore"),
            null,
            null, null,
            Set.of("minecraft:turtle_egg")));

        // Aquatic Essence
        configs.add(new EssenceConfig("aquatic",
            Set.of("minecraft:river", "minecraft:ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean"),
            null,
            null, null,
            Set.of("minecraft:water_bucket")));

        // Kelp Essence
        configs.add(new EssenceConfig("kelp",
            Set.of("minecraft:ocean", "minecraft:cold_ocean", "minecraft:lukewarm_ocean"),
            null,
            null, null,
            Set.of("minecraft:kelp")));

        // Abyssal Essence
        configs.add(new EssenceConfig("abyssal",
            Set.of("minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_frozen_ocean"),
            null,
            null, null,
            Set.of("minecraft:heart_of_the_sea")));

        // Tropical Sea Essence
        configs.add(new EssenceConfig("tropical_sea",
            Set.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean"),
            null,
            null, null,
            Set.of("minecraft:tropical_fish")));

        // Frozen Sea Essence
        configs.add(new EssenceConfig("frozen_sea",
            Set.of("minecraft:frozen_ocean", "minecraft:deep_frozen_ocean"),
            null,
            null, null,
            Set.of("minecraft:blue_ice")));

        // Jungle Essence
        configs.add(new EssenceConfig("jungle",
            Set.of("minecraft:jungle", "minecraft:sparse_jungle"),
            null,
            null, null,
            Set.of("minecraft:cocoa_beans")));

        // Bamboo Essence
        configs.add(new EssenceConfig("bamboo",
            Set.of("minecraft:bamboo_jungle"),
            null,
            null, null,
            Set.of("minecraft:bamboo")));

        // Swamp Essence
        configs.add(new EssenceConfig("swamp",
            Set.of("minecraft:swamp"),
            null,
            null, null,
            Set.of("minecraft:lily_pad")));

        // Mangrove Essence
        configs.add(new EssenceConfig("mangrove",
            Set.of("minecraft:mangrove_swamp"),
            null,
            null, null,
            Set.of("minecraft:mangrove_propagule")));

        // Taiga Essence
        configs.add(new EssenceConfig("taiga",
            Set.of("minecraft:taiga", "minecraft:old_growth_spruce_taiga"),
            null,
            null, null,
            Set.of("minecraft:spruce_sapling")));

        // Old Growth Essence
        configs.add(new EssenceConfig("old_growth",
            Set.of("minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"),
            null,
            null, null,
            Set.of("minecraft:moss_block")));

        // Frostbitten Essence
        configs.add(new EssenceConfig("frostbitten",
            Set.of("minecraft:snowy_plains", "minecraft:snowy_beach", "minecraft:snowy_taiga", "minecraft:snowy_slopes"),
            null,
            null, null,
            Set.of("minecraft:powder_snow_bucket")));

        // Glacial Essence
        configs.add(new EssenceConfig("glacial",
            Set.of("minecraft:ice_spikes", "minecraft:frozen_peaks"),
            null,
            null, null,
            Set.of("minecraft:packed_ice")));

        // Alpine Essence
        configs.add(new EssenceConfig("alpine",
            Set.of("minecraft:grove", "minecraft:snowy_slopes"),
            null,
            null, null,
            Set.of("minecraft:sweet_berries")));

        // Peaks Essence
        configs.add(new EssenceConfig("peaks",
            Set.of("minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:frozen_peaks"),
            null,
            null, null,
            Set.of("minecraft:goat_horn")));

        // Windswept Essence
        configs.add(new EssenceConfig("windswept",
            Set.of("minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest"),
            null,
            null, null,
            Set.of("minecraft:emerald")));

        // Arid Essence
        configs.add(new EssenceConfig("arid",
            Set.of("minecraft:desert"),
            null,
            null, null,
            Set.of("minecraft:cactus")));

        // Terracotta Essence
        configs.add(new EssenceConfig("terracotta",
            Set.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"),
            null,
            null, null,
            Set.of("minecraft:terracotta")));

        // Gilded Essence
        configs.add(new EssenceConfig("gilded",
            Set.of("minecraft:badlands", "minecraft:eroded_badlands"),
            null,
            null, null,
            Set.of("minecraft:gold_nugget")));

        // Lush Essence
        configs.add(new EssenceConfig("lush",
            Set.of("minecraft:lush_caves"),
            null,
            null, null,
            Set.of("minecraft:glow_berries")));

        // Dripstone Essence
        configs.add(new EssenceConfig("dripstone",
            Set.of("minecraft:dripstone_caves"),
            null,
            null, null,
            Set.of("minecraft:pointed_dripstone")));

        // Sculk Essence
        configs.add(new EssenceConfig("sculk",
            Set.of("minecraft:deep_dark"),
            null,
            null, null,
            Set.of("minecraft:echo_shard")));

        // Mycelial Essence
        configs.add(new EssenceConfig("mycelial",
            Set.of("minecraft:mushroom_fields"),
            null,
            null, null,
            Set.of("minecraft:mushroom_stew")));

        // =============================================
        // NETHER
        // =============================================

        // Infernal Essence
        configs.add(new EssenceConfig("infernal",
            Set.of("minecraft:nether_wastes"),
            null,
            null, null,
            Set.of("minecraft:blaze_rod")));

        // Crimson Essence
        configs.add(new EssenceConfig("crimson",
            Set.of("minecraft:crimson_forest"),
            null,
            null, null,
            Set.of("minecraft:crimson_fungus")));

        // Warped Essence
        configs.add(new EssenceConfig("warped",
            Set.of("minecraft:warped_forest"),
            null,
            null, null,
            Set.of("minecraft:warped_fungus")));

        // Soulfire Essence
        configs.add(new EssenceConfig("soulfire",
            Set.of("minecraft:soul_sand_valley"),
            null,
            null, null,
            Set.of("minecraft:soul_sand")));

        // Basalt Essence
        configs.add(new EssenceConfig("basalt",
            Set.of("minecraft:basalt_deltas"),
            null,
            null, null,
            Set.of("minecraft:basalt")));

        // Hellish Essence
        configs.add(new EssenceConfig("hellish",
            Set.of("minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:soul_sand_valley"),
            null,
            null, null,
            Set.of("minecraft:wither_skeleton_skull")));

        // =============================================
        // END
        // =============================================

        // Ender Essence
        configs.add(new EssenceConfig("ender",
            Set.of("minecraft:the_end", "minecraft:small_end_islands"),
            null,
            null, null,
            Set.of("minecraft:ender_pearl")));

        // Chorus Essence
        configs.add(new EssenceConfig("chorus",
            Set.of("minecraft:end_highlands", "minecraft:end_midlands"),
            null,
            null, null,
            Set.of("minecraft:chorus_fruit")));

        // End Void Essence
        configs.add(new EssenceConfig("end_void",
            Set.of("minecraft:end_barrens", "minecraft:end_midlands", "minecraft:end_highlands"),
            null,
            null, null,
            Set.of("minecraft:shulker_shell")));

        // Void Essence
        configs.add(new EssenceConfig("void",
            Set.of("minecraft:the_void"),
            null,
            null, null,
            Set.of("minecraft:ender_eye")));

        return new EssenceDataJsonConfig(configs);
    }

    private static String serializeConfigs(Map<String, EssenceConfig> configs)
    {
        JsonObject root = new JsonObject();
        JsonArray essenceArray = new JsonArray();

        for (String essenceId : configs.keySet())
        {
            if(essenceId == null || essenceId.isEmpty()) continue;
            EssenceConfig config = configs.get(essenceId);
            JsonObject essenceObj = new JsonObject();
            essenceObj.addProperty("id", essenceId);

            if (config.biomes != null) {
                JsonArray biomesArray = new JsonArray();
                config.biomes.forEach(biomesArray::add);
                essenceObj.add("biomes", biomesArray);
            }

            if (config.entities != null) {
                JsonArray entitiesArray = new JsonArray();
                config.entities.forEach(entitiesArray::add);
                essenceObj.add("entities", entitiesArray);
            }

            if (config.structures != null) {
                JsonArray dimensionsArray = new JsonArray();
                config.structures.forEach(dimensionsArray::add);
                essenceObj.add("structures", dimensionsArray);
            }

            if (config.levelIds != null) {
                JsonArray levelsArray = new JsonArray();
                config.levelIds.forEach(levelsArray::add);
                essenceObj.add("dimensions", levelsArray);
            }

            if (config.items != null) {
                JsonArray itemsArray = new JsonArray();
                config.items.forEach(itemsArray::add);
                essenceObj.add("items", itemsArray);
            }

            essenceArray.add(essenceObj);
        }

        root.add("essenceConfigs", essenceArray);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
    }
}
