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

        // Plains Essence
        configs.add(new EssenceConfig("plains",
            Set.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"),
            Set.of("minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken", "minecraft:horse", "minecraft:donkey"),
            null, null, 
            Set.of("minecraft:dirt", "minecraft:grass_block")));

        // Jungle Essence
        configs.add(new EssenceConfig("jungle",
            Set.of("minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"),
            Set.of("minecraft:parrot", "minecraft:ocelot", "minecraft:panda"),
            null, null, 
            Set.of("minecraft:jungle_log", "minecraft:bamboo", "minecraft:cocoa_beans", "minecraft:vine")));

        // Swampy Essence
        configs.add(new EssenceConfig("swampy",
            Set.of("minecraft:swamp", "minecraft:mangrove_swamp"),
            Set.of("minecraft:slime", "minecraft:frog", "minecraft:witch"),
            null, null, 
            Set.of("minecraft:slime_ball", "minecraft:lily_pad", "minecraft:mangrove_log", "minecraft:mud")));

        // Cold Essence
        configs.add(new EssenceConfig("cold",
            Set.of("minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:snowy_beach", "minecraft:frozen_river", "minecraft:frozen_peaks", "minecraft:snowy_slopes"),
            Set.of("minecraft:stray", "minecraft:polar_bear", "minecraft:snow_golem"),
            null, null, 
            Set.of("minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snowball")));

        // Snowy Essence
        configs.add(new EssenceConfig("snowy",
            Set.of("minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:snowy_beach", "minecraft:frozen_river", "minecraft:grove"),
            Set.of("minecraft:polar_bear", "minecraft:snow_golem", "minecraft:fox", "minecraft:rabbit"),
            null, null, 
            Set.of("minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow_bucket", "minecraft:spruce_log")));

        // Ocean Essence
        configs.add(new EssenceConfig("ocean",
            Set.of("minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean", "minecraft:frozen_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_frozen_ocean"),
            Set.of("minecraft:squid", "minecraft:dolphin", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:drowned", "minecraft:guardian"),
            null, null, 
            Set.of("minecraft:water_bucket", "minecraft:kelp", "minecraft:sea_pickle", "minecraft:prismarine")));

        // Peaks Essence
        configs.add(new EssenceConfig("peaks",
            Set.of("minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest"),
            Set.of("minecraft:goat", "minecraft:llama"),
            null, null, 
            Set.of("minecraft:stone", "minecraft:cobblestone", "minecraft:gravel", "minecraft:emerald")));

        // Deep Essence
        configs.add(new EssenceConfig("deep",
            Set.of("minecraft:deep_dark", "minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_frozen_ocean"),
            Set.of("minecraft:warden", "minecraft:elder_guardian", "minecraft:guardian"),
            null, null, 
            Set.of("minecraft:sculk", "minecraft:sculk_catalyst", "minecraft:echo_shard", "minecraft:deepslate")));

        // Sunny Essence
        configs.add(new EssenceConfig("sunny",
            Set.of("minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:savanna", "minecraft:sunflower_plains"),
            Set.of("minecraft:rabbit", "minecraft:husk"),
            null, null, 
            Set.of("minecraft:sand", "minecraft:sandstone", "minecraft:cactus", "minecraft:dead_bush")));

        // Maple Essence
        configs.add(new EssenceConfig("maple",
            Set.of("minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:flower_forest"),
            Set.of("minecraft:bee", "minecraft:wolf"),
            null, null, 
            Set.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:honey_bottle", "minecraft:sweet_berries")));

        // Cherry Essence
        configs.add(new EssenceConfig("cherry",
            Set.of("minecraft:cherry_grove"),
            Set.of("minecraft:bee", "minecraft:pig", "minecraft:sheep"),
            null, null, 
            Set.of("minecraft:cherry_log", "minecraft:pink_petals", "minecraft:honeycomb", "minecraft:cherry_leaves")));

        // Mushroom Essence
        configs.add(new EssenceConfig("mushroom",
            Set.of("minecraft:mushroom_fields"),
            Set.of("minecraft:mooshroom"),
            null, null, 
            Set.of("minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:mushroom_stew", "minecraft:mycelium")));

        // Witching Essence
        configs.add(new EssenceConfig("witching",
            Set.of("minecraft:swamp", "minecraft:dark_forest"),
            Set.of("minecraft:witch", "minecraft:bat", "minecraft:spider", "minecraft:cave_spider"),
            null, null, 
            Set.of("minecraft:spider_eye", "minecraft:fermented_spider_eye", "minecraft:potion", "minecraft:dark_oak_log")));

        // Fire Essence
        configs.add(new EssenceConfig("fire",
            Set.of("minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:soul_sand_valley", "minecraft:basalt_deltas"),
            Set.of("minecraft:blaze", "minecraft:magma_cube", "minecraft:ghast"),
            Set.of("minecraft:the_nether"), null, 
            Set.of("minecraft:blaze_rod", "minecraft:fire_charge", "minecraft:magma_cream", "minecraft:lava_bucket")));

        // Hot Essence
        configs.add(new EssenceConfig("hot",
            Set.of("minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:savanna", "minecraft:nether_wastes", "minecraft:basalt_deltas"),
            Set.of("minecraft:blaze", "minecraft:strider", "minecraft:husk"),
            null, null, 
            Set.of("minecraft:sand", "minecraft:terracotta", "minecraft:magma_block", "minecraft:netherrack")));

        // Hellish Essence
        configs.add(new EssenceConfig("hellish",
            Set.of("minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:soul_sand_valley", "minecraft:basalt_deltas"),
            Set.of("minecraft:blaze", "minecraft:ghast", "minecraft:magma_cube", "minecraft:zombified_piglin", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:hoglin", "minecraft:wither_skeleton"),
            Set.of("minecraft:the_nether"), null, 
            Set.of("minecraft:netherrack", "minecraft:soul_sand", "minecraft:wither_skeleton_skull", "minecraft:nether_wart")));

        // Void Essence
        configs.add(new EssenceConfig("void",
            Set.of("minecraft:the_end", "minecraft:end_highlands", "minecraft:end_midlands", "minecraft:end_barrens", "minecraft:small_end_islands"),
            Set.of("minecraft:enderman", "minecraft:endermite", "minecraft:shulker", "minecraft:ender_dragon"),
            Set.of("minecraft:the_end"), null, 
            Set.of("minecraft:end_stone", "minecraft:ender_pearl", "minecraft:shulker_shell", "minecraft:chorus_fruit")));

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
