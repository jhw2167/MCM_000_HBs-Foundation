package com.holybuckets.foundation.config;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.config.model.EssenceDataJsonConfig;
import com.holybuckets.foundation.core.EssenceType;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.server.ServerStartingEvent;
import com.holybuckets.foundation.event.balm.server.ServerStoppedEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Singleton configuration class for mod settings and data
 */
public class ModConfig {
    
    private static ModConfig INSTANCE;
    public final Map<String, Set<Identifier>> essenceData;
    public final Map<Item, String> enchantedEssenceItemMap;
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }
    
    private ModConfig() {
        this.essenceData = new ConcurrentHashMap<>();
        this.enchantedEssenceItemMap = new HashMap<>();
    }

    public static void init(EventRegistrar registrar)
    {
        INSTANCE = ModConfig.getInstance();
        registrar.registerOnBeforeServerStarted(ModConfig::onBeforeServerStarted, EventPriority.Lowest);
        registrar.registerOnServerStopped(ModConfig::onServerStopped, EventPriority.Lowest);
    }
    private void onServerStopped() {
        INSTANCE = null;
    }

    private void onBeforeServerStarted()
    {
        // Load essence data from config
        String pathName =  PerformanceImpactConfig.getActive().configFiles.essenceConfigFilePath;
        File configFile = new File( pathName );
        File defaultConfigFile = new File(EssenceDataJsonConfig.DEF_ESSENCE_FILE_CONFIG_PATH );
        String jsonEssenceData = HBUtil.FileIO.loadJsonConfigs( configFile, defaultConfigFile, EssenceDataJsonConfig.DEFAULT_CONFIG );

        EssenceDataJsonConfig essenceDataConfig = new EssenceDataJsonConfig(jsonEssenceData);
        loadEssenceData(essenceDataConfig);

        EssenceType.init();
    }

    private void loadEssenceData(EssenceDataJsonConfig configJson)
    {
        Set<String> essenceIds = configJson.getAllEssenceIds();
        for( String id : essenceIds )
        {
            EssenceDataJsonConfig.EssenceConfig entry = configJson.getConfig( id );
            if(entry.all.isEmpty()) continue;
            essenceData.put( id, entry.all.stream().map(this::toResourceLocation).collect( Collectors.toSet() ) );

            if(entry.items==null || entry.items.isEmpty()) continue;
            for( String itemName : entry.items ) {
                Item item = HBUtil.ItemUtil.itemNameToItem( addNameSpaceMap(itemName) );
                if( item != null && !item.equals(Items.AIR) ) enchantedEssenceItemMap.put( item, id );
            }
        }
    }
        private String addNameSpaceMap(String name) {
            return (!name.contains(":")) ? "minecraft:" + name : name;
        }

        private Identifier toResourceLocation(String name) {
            return HBUtil.LOC(addNameSpaceMap(name));
        }

    @Nullable
    public Set<Identifier> getEssenceData(String key) {
        return essenceData.get(key);
    }

    @Nullable
    public String getEssence(Identifier loc) {
        for (Map.Entry<String, Set<Identifier>> entry : essenceData.entrySet()) {
            if (entry.getValue().contains(loc)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    public String getEssence(Item item) {
        return enchantedEssenceItemMap.get(item);
    }

    @Nullable
    public boolean hasEssence(String essenceName, Identifier loc) {
        Set<Identifier> essenceSet = essenceData.get(essenceName);
        if(essenceSet != null) return essenceSet.contains(loc);
        return false;
    }


    //** Events
    private static void onServerStopped(ServerStoppedEvent event) {
        getInstance().onServerStopped();
    }

    private static void onBeforeServerStarted(ServerStartingEvent event) {
        getInstance().onBeforeServerStarted();
    }




}
