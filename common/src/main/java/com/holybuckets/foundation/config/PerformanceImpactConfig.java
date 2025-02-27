package com.holybuckets.foundation.config;

import net.blay09.mods.balm.api.Balm;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.holybuckets.foundation.config.PerformanceImpactLevel.*;

/**
 * Tracking various settings that impact performance and enabling the ability
 * to adjust these value dynamically in game.
 */
public class PerformanceImpactConfig {

    public static final String CLASS_ID = "001";

    public static PerformanceImpactConfigData getActive() {
        return Balm.getConfig().getActive(PerformanceImpactConfigData.class);
    }

    public static void initialize() {
        Balm.getConfig().registerConfig(PerformanceImpactConfigData.class, null);
    }

    PerformanceImpactLevel performanceImpactLevel;
    AtomicInteger blockWritesPerTick;

    public PerformanceImpactConfig() {
        this( PerformanceImpactLevel.valueOf(getActive().performanceImpactConfig.performanceImpact) );
    }

    public PerformanceImpactConfig(PerformanceImpactLevel p ) {
        this.performanceImpactLevel = p;
        setBlockWritesPerTick( p );
    }


    //SETTERS
    public void setBlockWritesPerTick(PerformanceImpactLevel p)
    {
       if( p == null ) return;
       if( blockWritesPerTick == null)
            blockWritesPerTick = new AtomicInteger(0);

       if(p == VERY_LOW) {
              blockWritesPerTick.set(25);
         } else if(p == LOW) {
              blockWritesPerTick.set(100);
         } else if(p == MEDIUM) {
              blockWritesPerTick.set(250);
         } else if(p == HIGH) {
              blockWritesPerTick.set(500);
         } else if(p == VERY_HIGH) {
              blockWritesPerTick.set(1000);
       }
    }


    //GETTERS
    public AtomicInteger getBlockWritesPerTick() {
        return blockWritesPerTick;
    }

}
