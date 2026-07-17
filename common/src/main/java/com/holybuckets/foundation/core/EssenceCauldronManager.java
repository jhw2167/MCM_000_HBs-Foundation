package com.holybuckets.foundation.core;

import com.holybuckets.foundation.CommonClass;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.block.EssenceCauldronBlock;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private final Map<Player, Integer> playerPortalCount = new HashMap<>();
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

    public void addCauldron(BlockPos cauldronPos, EssenceType type)
    {
        if (cauldrons.containsKey(cauldronPos)) {
            EssenceCauldronData data = cauldrons.get(cauldronPos);
            data.endTick+= ESSENCE_CAULDRON_DURATION;
        }
        cauldrons.put(cauldronPos, new EssenceCauldronData(
            (ServerLevel) level, type, CONFIG.getTotalTickCount(), cauldronPos
        ));
    }


    //** CORE
    private boolean hasActiveCauldrons() {
        return !cauldrons.isEmpty();
    }

    private void onLevelTick()
    {
        List<ServerPlayer> players = HBUtil.PlayerUtil.getAllPlayers();
        List<BlockPos> cauldronPos = cauldrons.keySet().stream().toList();
        for (BlockPos pos : cauldronPos)
        {
             EssenceCauldronData data = cauldrons.get(pos);
             if(data == null) continue;

             if(data.getTeleportPos() == null) {
                 data.cauldronPreTeleportEffects();
                 data.onTickSetBiomeTargetPos();
             }

             if(data.startTick >= CONFIG.getTotalTickCount()) continue;

            if(data.getTotalBiomes() == 0) {
                data.cauldronFailedTeleportEffects();
                cauldrons.remove(pos);
                continue;
            }

            if(data.getTeleportPos() == null)
                data.setSafeTeleportPos();

             if (data.endTick <= CONFIG.getTotalTickCount()) {
                 cauldrons.remove(pos);
                 data.returnCauldronToNormal();
                 playerPortalCount.clear();
                 continue;
             }

             if(data.getTeleportPos()==null) continue;
            data.cauldronPostTeleportEffects();

             Map.Entry<BlockPos, Holder<Biome>> tpData = data.getTeleportPos();
             for(ServerPlayer player : players) {
                if( player.level().equals(level) && player.blockPosition().closerThan(pos, 2))
                {
                    handlePlayerTeleport(player, tpData);
                }
             }

        }

    }

    private static final int TP_TRANSITION_TICKS = 40;
    private void handlePlayerTeleport(ServerPlayer player, Map.Entry<BlockPos, Holder<Biome>> tpData)
    {
        if(!playerPortalCount.containsKey(player)) {
            playerPortalCount.put(player, 0);
            return;
        } else if (playerPortalCount.get(player) < TP_TRANSITION_TICKS) {
            playerPortalCount.put(player, playerPortalCount.get(player) + 1);
            return;
        }

        BlockPos tp = tpData.getKey();
        BlockPos playerStart = player.blockPosition();
        player.teleportTo(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5);

        String tpBiome = HBUtil.LevelUtil.getBiomeSimpleName(tpData.getValue());
        CommonClass.MESSAGER.sendBottomActionHint(player,
            Component.translatable("item.hbs_foundation.enchanted_essence.teleport_success",
                tpBiome).getString() );

        BlockPos playerEnd = player.blockPosition();
        int blockDist = HBUtil.BlockUtil.distanceSqr(playerStart, playerEnd);
        String dist = (int) Math.sqrt(blockDist) + "";
        /*
        CommonClass.MESSAGER.sendChat(player,
            Component.translatable("item.hbs_foundation.enchanted_essence.teleport_success_chat",
                HBUtil.BlockUtil.positionToString(playerStart),
                HBUtil.BlockUtil.positionToString(playerEnd),
                dist).getString());

         */

    }


    //** STATICS
    private static Set<Player> errorPlayers = new HashSet<>();
    public static void essenceCauldronErrorMsgCooldown(Player player) {
        errorPlayers.add(player);
    }

    public static boolean hasEssenceCauldron(Level level, BlockPos cauldronPos) {
        EssenceCauldronManager manager = EssenceCauldronManager.get(level);
        if(manager == null) return false;
        return manager.hasCauldron(cauldronPos);
    }

    public static void addEssenceCauldron(Level level, BlockPos cauldronPos, EssenceType type) {
        EssenceCauldronManager manager = EssenceCauldronManager.get(level);
        if(manager == null) return;
        manager.addCauldron(cauldronPos, type);
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
        if(manager.hasActiveCauldrons())
            manager.onLevelTick();


        if(CONFIG.getTotalTickCount()%120L == 0)
            errorPlayers.clear();
    }

    //** PLACEHOLDER DATA CLASS

    public static final long ESSENCE_CAULDRON_START_DELAY = 36L;
    public static final long ESSENCE_CAULDRON_DURATION = 200L; //ticks
    public static class EssenceCauldronData {

        ServerLevel level;
        EssenceType essenceType;
        Map<Holder<Biome>, BlockPos> targetBiomePos;
        BlockPos safeTeleportPos;
        BlockPos startPos;
        private int particleTick;
        final long startTick;
        long endTick;
        final int totalBiomes;
        int searchOffset;
        Holder<Biome> teleportBiome;
        BlockState cauldronState;


        EssenceCauldronData(ServerLevel level, EssenceType type, long startTick, BlockPos startPos) {
            this.level = level;
            this.startPos = startPos;
            this.particleTick = 0;
            this.startTick = startTick + ESSENCE_CAULDRON_START_DELAY;
            this.endTick = this.startTick + ESSENCE_CAULDRON_DURATION;
            this.essenceType = type;
            this.targetBiomePos = new HashMap<>();
            this.totalBiomes = type.getBiomes().size();
            this.searchOffset = 0;
        }


        public Map.Entry<BlockPos, Holder<Biome>> getTeleportPos() {
            if(teleportBiome == null || safeTeleportPos == null) return null;
            return new AbstractMap.SimpleEntry<>(safeTeleportPos, teleportBiome);
        }

        public int getTotalBiomes() {
            return totalBiomes;
        }

        private static final Vec3i[] BIOME_SEARCH_OFFSETS = {
            Vec3i.ZERO, Vec3i.ZERO, Vec3i.ZERO, Vec3i.ZERO,
            new Vec3i( 6400,  0,     0),  // East
            new Vec3i(-6400,  0,     0),  // West
            new Vec3i(    0,  0,  6400),  // South
            new Vec3i(    0,  0, -6400),  // North
            new Vec3i( 6400,  0,  6400),  // Southeast
            new Vec3i(-6400,  0,  6400),  // Southwest
            new Vec3i( 6400,  0, -6400),  // Northeast
            new Vec3i(-6400,  0, -6400),  // Northwest
        };

        final static int SEARCH_RATE_TICKS = 5;
        public void onTickSetBiomeTargetPos()
        {
            //check if we already have all the biomes
            if( totalBiomes==0 ) return;
            if(targetBiomePos.size() >= totalBiomes) return;
            if(particleTick % SEARCH_RATE_TICKS != 0) return;
            if(targetBiomePos.size() > 0 && searchOffset == 3) {
                searchOffset = BIOME_SEARCH_OFFSETS.length; //find closest, disable search
                //otherwise, we want to find all possible biomes in outer ring
                return;
            }
            if(totalBiomes >= 4 && targetBiomePos.size() > 2) return;
            if(searchOffset >= BIOME_SEARCH_OFFSETS.length) return;

            Predicate<Holder<Biome>> biomePredicate = biome ->
                !targetBiomePos.containsKey(biome) && essenceType.matchesBiome(biome);

            BlockPos searchPos = startPos.offset(BIOME_SEARCH_OFFSETS[searchOffset++]);
            Pair<BlockPos, Holder<Biome>> res = level.findClosestBiome3d(
                biomePredicate, searchPos, 6400, 32, 128 //vertical resolution
            );

            if (res != null) {
                targetBiomePos.put(res.getSecond(), res.getFirst());
            }
        }

        public void setSafeTeleportPos()
        {
            //find any from targetBiomePos that is in a loaded chunk and has sky access, set as teleport pos
            var opt = targetBiomePos.keySet().stream().findAny();
            if(opt.isEmpty()) return;
            teleportBiome = opt.get();
            BlockPos targetPos = targetBiomePos.get(teleportBiome);
            ChunkAccess chunk = level.getChunk(targetPos);

            if(chunk == null) return;
            BlockPos safePos = targetPos;
            while(!level.canSeeSky(safePos) && level.getMaxBuildHeight() > safePos.getY()) {
                safePos = safePos.above();
            }
            this.safeTeleportPos = safePos;
        }


        private void spawnParticles(List<Object[]> particles) {
            ServerLevel serverLevel = (ServerLevel) level;
            for (Object[] p : particles) {
                serverLevel.sendParticles(
                    (ParticleOptions) p[0],
                    ((Number) p[1]).doubleValue(),
                    ((Number) p[2]).doubleValue(),
                    ((Number) p[3]).doubleValue(),
                    1,
                    ((Number) p[4]).doubleValue(),
                    ((Number) p[5]).doubleValue(),
                    ((Number) p[6]).doubleValue(),
                    0.0
                );
            }
        }

        private double rx() { return startPos.getX() + 0.5 + (Math.random() - 0.5) * 0.6; }
        private double rz() { return startPos.getZ() + 0.5 + (Math.random() - 0.5) * 0.6; }
        private double sy() { return startPos.getY() + 1.0; }

        public void cauldronPreTeleportEffects()
        {
            particleTick++;

            // Bias toward bubbles early, shift toward witch particles over time
            double witchChance = Math.min(0.6, particleTick / 200.0);

            List<Object[]> particles = new ArrayList<>();

            // Always at least one bubble
            particles.add(new Object[]{ ParticleTypes.BUBBLE_COLUMN_UP, rx(), sy(), rz(), 0.0, 0.1 + Math.random() * 0.05, 0.0 });

            // Random extra bubble
            if (Math.random() > 0.4) {
                particles.add(new Object[]{ ParticleTypes.BUBBLE_COLUMN_UP, rx(), sy(), rz(), (Math.random()-0.5)*0.05, 0.08, (Math.random()-0.5)*0.05 });
            }

            // Witch particles grow in frequency over time
            if (Math.random() < witchChance) {
                particles.add(new Object[]{ ParticleTypes.WITCH, rx(), sy() + Math.random() * 0.3, rz(), 0.0, 0.05, 0.0 });
            }
            if (Math.random() < witchChance * 0.6) {
                particles.add(new Object[]{ ParticleTypes.WITCH, rx(), sy() + Math.random() * 0.5, rz(), 0.0, 0.03, 0.0 });
            }

            spawnParticles(particles);
        }

        public void cauldronPostTeleportEffects()
        {

            // Swap block to EssenceCauldronBlock if not already
            if (!(level.getBlockState(startPos).getBlock() instanceof EssenceCauldronBlock)) {
                cauldronState = level.getBlockState(startPos);
                level.setBlock(startPos,
                    ModBlocks.essenceCauldron.get().defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 3), 3
                );
            }

            List<Object[]> particles = new ArrayList<>();

            // Calm witch particles — sparse, drifting upward
            particles.add(new Object[]{ ParticleTypes.WITCH, rx(), sy() + Math.random() * 0.4, rz(), 0.0, 0.02, 0.0 });

            if (Math.random() < 0.5) {
                particles.add(new Object[]{ ParticleTypes.WITCH, rx(), sy() + Math.random() * 0.6, rz(), 0.0, 0.015, 0.0 });
            }

            // Occasional bubble — reaction has settled
            if (Math.random() < 0.2) {
                particles.add(new Object[]{ ParticleTypes.BUBBLE_COLUMN_UP, rx(), sy(), rz(), 0.0, 0.04, 0.0 });
            }

            // Experience orbs drifting up
            if (Math.random() < 0.4) {
                particles.add(new Object[]{ ParticleTypes.DRAGON_BREATH , rx(), sy() + Math.random() * 0.3, rz(), 0.0, 0.03, 0.0 });
            }
            if (Math.random() < 0.2) {
                particles.add(new Object[]{ ParticleTypes.DRAGON_BREATH, rx(), sy() + Math.random() * 0.5, rz(), 0.0, 0.02, 0.0 });
            }

            spawnParticles(particles);
        }

        public void returnCauldronToNormal() {
            if (cauldronState != null) {
                level.setBlock(startPos, cauldronState, 3);
            }
        }

        /**
         * Add some floating redstone particles to indicate the spell failed
         */
         private static ParticleOptions RED_FAIL_DUST = new DustParticleOptions(
             new Vector3f(0.8f, 0.1f, 0.1f), 1.0f);
        public void cauldronFailedTeleportEffects() {
            List<Object[]> particles = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                particles.add(new Object[]{ RED_FAIL_DUST , rx(), sy() + Math.random() * 0.5, rz(), (Math.random()-0.5)*0.1, 0.02 + Math.random() * 0.02, (Math.random()-0.5)*0.1 });
            }
            spawnParticles(particles);
        }
    }
}