package com.holybuckets.foundation.config;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.Config;

@Config(Constants.MOD_ID)
public class PerformanceImpactConfigData implements BalmConfigData {
    public CPerformanceImpactConfig performanceImpactConfig = new CPerformanceImpactConfig();

    public static class CPerformanceImpactConfig {

        @Comment("Dictates the performance impact of any Holy Buckets mods by controlling settings that influence operations per second or number of threads. Options are VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH")
        public String performanceImpact = "MEDIUM";
    }
}
