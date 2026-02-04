package com.holybuckets.foundation.config;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Singleton configuration class for mod settings and data
 */
public class ModConfig {
    
    private static ModConfig INSTANCE;
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }
    
    private ModConfig() {
        this.essenceData = new HashMap<>();
    }
    
    /**
     * Map of essence data where each string key maps to a set of ResourceLocations
     */
    private final Map<String, Set<ResourceLocation>> essenceData;
    
    /**
     * Gets the essence data map
     * @return The essence data map
     */
    public Map<String, Set<ResourceLocation>> getEssenceData() {
        return essenceData;
    }
    
    /**
     * Adds a ResourceLocation to the specified essence key
     * @param key The essence key
     * @param location The ResourceLocation to add
     */
    public void addEssenceData(String key, ResourceLocation location) {
        essenceData.computeIfAbsent(key, k -> new HashSet<>()).add(location);
    }
    
    /**
     * Gets the set of ResourceLocations for the specified essence key
     * @param key The essence key
     * @return The set of ResourceLocations, or empty set if key doesn't exist
     */
    public Set<ResourceLocation> getEssenceData(String key) {
        return essenceData.getOrDefault(key, new HashSet<>());
    }
    
    /**
     * Removes a ResourceLocation from the specified essence key
     * @param key The essence key
     * @param location The ResourceLocation to remove
     * @return true if the location was removed, false if it wasn't present
     */
    public boolean removeEssenceData(String key, ResourceLocation location) {
        Set<ResourceLocation> locations = essenceData.get(key);
        if (locations != null) {
            return locations.remove(location);
        }
        return false;
    }
    
    /**
     * Clears all essence data for the specified key
     * @param key The essence key to clear
     */
    public void clearEssenceData(String key) {
        essenceData.remove(key);
    }
    
    /**
     * Clears all essence data
     */
    public void clearAllEssenceData() {
        essenceData.clear();
    }
}
