package com.holybuckets.foundation.config;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.platform.config.reflection.Comment;
import net.blay09.mods.balm.platform.config.reflection.Config;

@Config(Constants.MOD_ID)
public class PerformanceImpactConfigData {
    public CPerformanceImpactConfig performanceImpactConfig = new CPerformanceImpactConfig();
    public ConfigFiles configFiles = new ConfigFiles();

    public static class ConfigFiles {
        @Comment("File path to EnchantedEssence config file")
        public String essenceConfigFilePath = "config/HBEnchantedEssenceConfig.json";
    }

    public static class CPerformanceImpactConfig {

        @Comment("Dictates the performance impact of any Holy Buckets mods by controlling settings that influence operations per second or number of threads. Options are VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH")
        public String performanceImpact = "MEDIUM";
    }
}
