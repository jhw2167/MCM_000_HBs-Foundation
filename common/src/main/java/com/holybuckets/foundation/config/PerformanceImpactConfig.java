package com.holybuckets.foundation.config;

import net.blay09.mods.balm.api.Balm;

import java.util.concurrent.atomic.AtomicBoolean;
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
    AtomicInteger chunkExploreMaximumDiskSize;
    AtomicBoolean enableChunkExplorer;

    public PerformanceImpactConfig() {
        this( PerformanceImpactLevel.valueOf(getActive().performanceImpactConfig.performanceImpact) );
    }

    public PerformanceImpactConfig(PerformanceImpactLevel p ) {
        this.performanceImpactLevel = p;
        setBlockWritesPerTick( p );
        setChunkExploreRate( getActive().features.chunkExploreIdleTime);
        setChunkExploreMaxAllowedTickSize( getActive().features.chunkExploreMaxAllowedTickSize );
        setChunkExploreMaximumDiskSize( getActive().features.chunkExploreMaximumDiskSize );
        setEnableChunkExplorer( getActive().features.enableChunkExplorer );
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

    public void setEnableChunkExplorer(boolean enabled) {
        if( enableChunkExplorer == null)
            enableChunkExplorer = new AtomicBoolean(false);
        enableChunkExplorer.set(enabled);
    }

    /**
     * Session value. Seeded from config on load, then changed freely in game or by a dependent mod.
     */
    public boolean isEnableChunkExplorer() {
        if( enableChunkExplorer == null)
            setEnableChunkExplorer( getActive().features.enableChunkExplorer );
        return enableChunkExplorer.get();
    }

    public void setChunkExploreMaximumDiskSize(int gigabytes) {
        if( chunkExploreMaximumDiskSize == null)
            chunkExploreMaximumDiskSize = new AtomicInteger(8);
        chunkExploreMaximumDiskSize.set(gigabytes);
    }

    /**
     * Session value in gigabytes. Seeded from config on load, then changed freely in game.
     */
    public int getChunkExploreMaximumDiskSize() {
        if( chunkExploreMaximumDiskSize == null)
            setChunkExploreMaximumDiskSize( getActive().features.chunkExploreMaximumDiskSize );
        return chunkExploreMaximumDiskSize.get();
    }

    public int getChunkExploreRate() {
        if( chunkExploreRate == null)
            setChunkExploreRate( getActive().features.chunkExploreIdleTime);
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
