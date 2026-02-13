package com.holybuckets.foundation.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages magic cauldrons that an Enchanted Essence has triggered.
 * One instance exists per loaded level.
 */
public class EssenceCauldronManager {

    public static final String CLASS_ID = "036";

    private final Level level;

    // Active cauldron positions in this level
    private final Map<BlockPos, EssenceCauldronData> cauldrons = new HashMap<>();
    private static final Map<Level, EssenceCauldronManager> managers = new HashMap<>();
    private static GeneralConfig CONFIG;

    private EssenceCauldronManager(Level level) {
        this.level = level;
    }

    //** GETTERS

    public Map<BlockPos, EssenceCauldronData> getCauldrons() {
        return cauldrons;
    }

    public EssenceCauldronData getCauldron(BlockPos pos) {
        return cauldrons.get(pos);
    }

    public boolean hasCauldron(BlockPos pos) {
        return cauldrons.containsKey(pos);
    }

    //** MUTATION

    public void addCauldron(BlockPos pos) {
        if (cauldrons.containsKey(pos)) {
            EssenceCauldronData data = cauldrons.get(pos);
            data.endTick+= ESSENCE_CAULDRON_DURATION;
        }
        cauldrons.put(pos, new EssenceCauldronData(level, pos, CONFIG.getTotalTickCount()) );
    }


    //** CORE
    private boolean hasActiveCauldrons() {
        return !cauldrons.isEmpty();
    }

    private void onLevelTick()
    {
        List<ServerPlayer> players = HBUtil.PlayerUtil.getAllPlayers();
        List<BlockPos> cauldronPos = cauldrons.keySet().stream().toList();
        for (BlockPos pos : cauldronPos) {
             EssenceCauldronData data = cauldrons.get(pos);
             if(data == null) continue;
             if (data.endTick <= CONFIG.getTotalTickCount()) {
                 cauldrons.remove(pos);
                 continue;
             }
             if(data.getTeleportPos()==null) continue;
             BlockPos tp = data.getTeleportPos();
             for(ServerPlayer player : players) {
                if( player.level().equals(level) && player.blockPosition().closerThan(pos, 2)) {
                    player.teleportTo(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5);
                }
             }

        }
    }


    //** STATICS
    public static void addEssenceCauldron(Level level, BlockPos pos) {
        EssenceCauldronManager manager = EssenceCauldronManager.get(level);
        if(manager == null) return;
        manager.addCauldron(pos);
    }

    private static EssenceCauldronManager init(Level level) {
        if (!managers.containsKey(level)) {
            managers.put(level, new EssenceCauldronManager(level));
        }
        return managers.get(level);
    }

    public static EssenceCauldronManager get(Level level) {
        if (GeneralConfig.getInstance().isIntegrated()) {
            level = HBUtil.LevelUtil.toLevel(HBUtil.LevelUtil.LevelNameSpace.SERVER, level.dimension());
        }
        if (!managers.containsKey(level))
            init(level);
        return managers.get(level);
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(EssenceCauldronManager::onServerStart);
        reg.registerOnLevelLoad(EssenceCauldronManager::onLevelLoad);
        reg.registerOnLevelUnload(EssenceCauldronManager::onLevelUnload);

        reg.registerOnServerLevelTick(EssenceCauldronManager::onServerLevelTick);
    }

    private static void onServerStart(ServerStartingEvent event) {
        managers.clear();
        CONFIG = GeneralConfig.getInstance();
    }

    private static void onLevelLoad(LevelLoadingEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        EssenceCauldronManager.init((Level) event.getLevel());
    }

    private static void onLevelUnload(LevelLoadingEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        managers.remove(event.getLevel());
    }

    private static void onServerLevelTick(Level level) {
        if(level.isClientSide()) return;
        if(!EssenceCauldronManager.managers.containsKey(level)) return;
        EssenceCauldronManager manager = EssenceCauldronManager.get(level);
        if(!manager.hasActiveCauldrons()) return;
        manager.onLevelTick();
    }

    //** PLACEHOLDER DATA CLASS

    public static final long ESSENCE_CAULDRON_DURATION = 200L; //ticks
    public static class EssenceCauldronData {
        final BlockPos targetBiomePos;
        BlockPos safeTeleportPos;
        final long startTick;
        long endTick;

        EssenceCauldronData(Level level, BlockPos targetBiomePos, long startTick) {
            this.startTick = startTick;
            this.endTick = startTick + ESSENCE_CAULDRON_DURATION;
            this.targetBiomePos = targetBiomePos;
            setSafeTeleportPos(level, targetBiomePos);
        }

        public void setSafeTeleportPos(Level level, BlockPos targetPos) {
            ChunkAccess chunk = level.getChunk(targetPos);
            if(chunk == null) return;
            BlockPos safePos = targetPos;
            while(!level.canSeeSky(safePos) && level.getMaxBuildHeight() > safePos.getY()) {
                safePos = safePos.above();
            }
            if(!level.canSeeSky(safePos)) return;
            this.safeTeleportPos = safePos;
        }

        public BlockPos getTeleportPos() {
            return safeTeleportPos;
        }

        public BlockPos getTargetBiomePos() {
            return targetBiomePos;
        }
    }
}