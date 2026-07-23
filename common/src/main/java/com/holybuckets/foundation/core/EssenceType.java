package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.config.ModConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashSet;
import java.util.Set;

public class EssenceType {
    private final String essenceName;

    // Static registry caches
    private static GeneralConfig GENERAL_CONFIG;
    private static ModConfig MOD_CONFIG;
    private static Registry<Biome> biomeRegistry;
    private static Registry<Item> itemRegistry;
    private static Registry<EntityType<?>> entityRegistry;
    private static Registry<Structure> structureRegistry;
    private static Registry<Level> levelRegistry;

    // Initialize registries from server
    public static void init()
    {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        MOD_CONFIG = MOD_CONFIG.getInstance();

        RegistryAccess registryAccess = GENERAL_CONFIG.getServer().registryAccess();
        biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        entityRegistry = registryAccess.registryOrThrow(Registries.ENTITY_TYPE);
        structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
        levelRegistry = registryAccess.registryOrThrow(Registries.DIMENSION);
        itemRegistry = registryAccess.registryOrThrow(Registries.ITEM);

    }

    public static EssenceType of(Item itemType) {
        String essenceName = MOD_CONFIG.getEssence(itemType);
        return (essenceName != null) ? new EssenceType(essenceName) : null;
    }

    public EssenceType(String essenceName) {
        this.essenceName = essenceName;
    }

    public String getEssenceId() {
        return essenceName;
    }

    public String getEssenceName() {
        return getEssenceName(essenceName);
    }

    public static String getEssenceName(String essenceId )
    {
        //to proper case
        String[] parts = essenceId.split("_");
        StringBuilder displayName = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                displayName.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    displayName.append(part.substring(1));
                }
                displayName.append(" ");
            }
        }
        return displayName.toString().trim();
    }

    private Set<Identifier> getEssenceLocations() {
        return MOD_CONFIG.getEssenceData(essenceName);
    }

    public static Set<Identifier> getEssenceLocations(String essenceName) {
        return MOD_CONFIG.getEssenceData(essenceName);
    }

    public boolean matches(Identifier loc) {
        return getEssenceLocations().contains(loc);
    }


    public static Set<Holder<Biome>> getBiomes(String essenceName)
    {
        if(essenceName == null) return Set.of();

        Set<Holder<Biome>> biomes = new HashSet<>();
        if (biomeRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.getBiomes() called before init()");
            return biomes;
        }

        for (Identifier loc : getEssenceLocations(essenceName)) {
            if (biomeRegistry.containsKey(loc)) {
                Biome b = biomeRegistry.get(loc);
                if(b != null) biomes.add(biomeRegistry.wrapAsHolder(b));
            }
        }
        return biomes;
    }

    public Set<EntityType<?>> getEntities() {
        Set<EntityType<?>> entities = new HashSet<>();
        if (entityRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.getEntities() called before init()");
            return entities;
        }

        for (Identifier loc : getEssenceLocations()) {
            if (entityRegistry.containsKey(loc)) {
                entities.add(entityRegistry.get(loc));
            }
        }
        return entities;
    }

    public Set<Structure> getStructures() {
        Set<Structure> structures = new HashSet<>();
        if (structureRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.getStructures() called before init()");
            return structures;
        }

        for (Identifier loc : getEssenceLocations()) {
            if (structureRegistry.containsKey(loc)) {
                structures.add(structureRegistry.get(loc));
            }
        }
        return structures;
    }

    public Set<Level> getDimensions() {
        Set<Level> dimensions = new HashSet<>();
        if (levelRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.getDimensions() called before init()");
            return dimensions;
        }

        for (Identifier loc : getEssenceLocations()) {
            if (levelRegistry.containsKey(loc)) {
                dimensions.add(levelRegistry.get(loc));
            }
        }
        return dimensions;
    }

    public Set<Holder<Biome>> getBiomes() {
       if(this.essenceName == null) return Set.of();
         return getBiomes(this.essenceName);
    }



    public boolean matchesBiome(Holder<Biome> biome) {
        if (biomeRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.matchesBiome() called before init()");
            return false;
        }

        Identifier biomeLoc = biomeRegistry.getKey(biome.value());
        return biomeLoc != null && matches(biomeLoc);
    }

    public boolean matchesEntity(EntityType<?> entityType) {
        if (entityRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.matchesEntity() called before init()");
            return false;
        }

        Identifier entityLoc = entityRegistry.getKey(entityType);
        return entityLoc != null && matches(entityLoc);
    }

    public boolean matchesStructure(Structure structure) {
        if (structureRegistry == null) {
            LoggerBase.logError( null, "05100", "EssenceType.matchesStructure() called before init()");
            return false;
        }

        Identifier structureLoc = structureRegistry.getKey(structure);
        return structureLoc != null && matches(structureLoc);
    }

    public boolean matchesDimension(Level dimension) {
        return matches(dimension.dimension().location());
    }

    public boolean matchesItem(Item item) {
        return MOD_CONFIG.hasEssence(essenceName, itemRegistry.getKey(item));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EssenceType)) return false;
        return essenceName.equals(((EssenceType) obj).essenceName);
    }

    @Override
    public int hashCode() {
        return essenceName.hashCode();
    }
}