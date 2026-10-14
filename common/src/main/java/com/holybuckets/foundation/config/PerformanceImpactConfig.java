package com.holybuckets.foundation.config;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
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
    AtomicInteger chunkExploreMaxAllocationRate;
    AtomicBoolean enableChunkExplorer;

    public PerformanceImpactConfig(PerformanceImpactConfigData data) {
        this( PerformanceImpactLevel.valueOf(data.performanceImpactConfig.performanceImpact) );
    }

    public PerformanceImpactConfig(PerformanceImpactLevel p ) {
        this.performanceImpactLevel = p;
        setBlockWritesPerTick( p );
        setChunkExploreRate( getActive().features.chunkExploreIdleTime);
        setChunkExploreMaxAllowedTickSize( getActive().features.chunkExploreMaxAllowedTickSize );
        setChunkExploreMaximumDiskSize( getActive().features.chunkExploreMaximumDiskSize );
        setChunkExploreMaxAllocationRate( getActive().features.chunkExploreMaxAllocationRate );
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

    public void setChunkExploreMaxAllocationRate(int megabytesPerSecond) {
        if( chunkExploreMaxAllocationRate == null)
            chunkExploreMaxAllocationRate = new AtomicInteger(800);
        chunkExploreMaxAllocationRate.set(megabytesPerSecond);
    }

    /**
     * Session value in megabytes per second. Seeded from config on load, then changed freely in game
     * or by a dependent mod.
     */
    public int getChunkExploreMaxAllocationRate() {
        if( chunkExploreMaxAllocationRate == null)
            setChunkExploreMaxAllocationRate( getActive().features.chunkExploreMaxAllocationRate );
        return chunkExploreMaxAllocationRate.get();
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


    //** ALLOCATION RATE
    public static void sampleMemory(ServerTickEvent event) {
        long mem = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();

        allocs.add((mem - free) / 1024 / 1024); // in MB
        if(allocs.size() > 120) allocs.removeFirst();
    }

    static final Deque<Long> allocs = new ArrayDeque<>(120);

    /** Records one sample, at most twice a second. Called from the server tick, single threaded. */
    public static int getAllocationRate(int seconds) {
        int sum = 0;
        if(allocs.isEmpty()) return 0;
        Iterator<Long> iterator = allocs.descendingIterator();
        for(int i = 0; i < seconds; i++) {
            if(!iterator.hasNext()) break;
            sum += iterator.next();
        }
        int avg = sum / allocs.size();

        return avg / seconds;
    }


}
