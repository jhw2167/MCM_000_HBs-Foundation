package com.holybuckets.foundation.config;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.Config;

@Config(Constants.MOD_ID)
public class PerformanceImpactConfigData implements BalmConfigData {
    public CPerformanceImpactConfig performanceImpactConfig = new CPerformanceImpactConfig();
    public ConfigFiles configFiles = new ConfigFiles();
    public CFeatures features = new CFeatures();

    public static class CFeatures {

        @Comment("Enables the Chunk Explorer, which pre-generates distant chunks around players in the background. Requires Chunk Pregenerator to be installed; ignored otherwise. Disabled by default: it generates world on the server thread while players are online.")
        public boolean enableChunkExplorer = false;

        @Comment("The rate of chunk exploration beyond player bounds. 100 is the fastest. Valid range 1 - 100.")
        public int chunkExploreRate = 1;

        @Comment("Disables background chunk loading once the chunk folder size on disk has exceed this size. Assumes 8KB per chunk. Takes integer value in Gigabytes")
        public int chunkExploreMaximumDiskSize = 8;
    }

    public static class ConfigFiles {
        @Comment("File path to EnchantedEssence config file")
        public String essenceConfigFilePath = "config/HBEnchantedEssenceConfig.json";
    }

    public static class CPerformanceImpactConfig {

        @Comment("Dictates the performance impact of any Holy Buckets mods by controlling settings that influence operations per second or number of threads. Options are VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH")
        public String performanceImpact = "MEDIUM";
    }
}
