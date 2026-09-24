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
    AtomicInteger chunkExploreRate;
    AtomicInteger chunkExploreMaxAllowedTickSize;

    public PerformanceImpactConfig() {
        this( PerformanceImpactLevel.valueOf(getActive().performanceImpactConfig.performanceImpact) );
    }

    public PerformanceImpactConfig(PerformanceImpactLevel p ) {
        this.performanceImpactLevel = p;
        setBlockWritesPerTick( p );
        setChunkExploreRate( getActive().features.chunkExploreRate );
        setChunkExploreMaxAllowedTickSize( getActive().features.chunkExploreMaxAllowedTickSize );
    }

    public void setChunkExploreRate(int rate) {
        if( chunkExploreRate == null)
            chunkExploreRate = new AtomicInteger(20);
        chunkExploreRate.set(rate);
    }

    public void setChunkExploreMaxAllowedTickSize(int millis) {
        if( chunkExploreMaxAllowedTickSize == null)
            chunkExploreMaxAllowedTickSize = new AtomicInteger(45);
        chunkExploreMaxAllowedTickSize.set(millis);
    }

    /**
     * Session value in milliseconds. Seeded from config on load, then changed freely in game.
     */
    public int getChunkExploreMaxAllowedTickSize() {
        if( chunkExploreMaxAllowedTickSize == null)
            setChunkExploreMaxAllowedTickSize( getActive().features.chunkExploreMaxAllowedTickSize );
        return chunkExploreMaxAllowedTickSize.get();
    }

    public int getChunkExploreMaximumDiskSize() {
        return getActive().features.chunkExploreMaximumDiskSize;
    }

    public int getChunkExploreRate() {
        return chunkExploreRate.get();
    }


    //SETTERS
    public void setBlockWritesPerTick(PerformanceImpactLevel p)
    {
       if( p == null ) return;
       if( blockWritesPerTick == null)
            blockWritesPerTick = new AtomicInteger(20);

       if(p == VERY_LOW) {
              blockWritesPerTick.set(5);
         } else if(p == LOW) {
              blockWritesPerTick.set(10);
         } else if(p == MEDIUM) {
              blockWritesPerTick.set(20);
         } else if(p == HIGH) {
              blockWritesPerTick.set(40);
         } else if(p == VERY_HIGH) {
              blockWritesPerTick.set(60);
       }
    }


    //GETTERS
    public AtomicInteger getBlockWritesPerTick() {
        return blockWritesPerTick;
    }

}
