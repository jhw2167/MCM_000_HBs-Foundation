package com.holybuckets.foundation.core;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.structure.StructureInfo;
import com.holybuckets.foundation.structure.StructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public class ChunkAnalyzer {

    public static final String CLASS_ID = "041";
    public static final int MAX_CACHE_SIZE = 256;
    public static final int STRUCTURE_SEARCH_BLOCKS = 32;

    public static final Set<Block> ORE_BLOCKS = Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE
    );

    public static final Set<Block> DEEPSLATE_ORE_BLOCKS = Set.of(
        Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_DIAMOND_ORE
    );

    private static final Map<Level, ChunkAnalyzer> analyzers = new HashMap<>();

    private final Level level;
    private final Map<ChunkPos, ChunkBlockDistro> cache;

    private ChunkAnalyzer(Level level) {
        this.level = level;
        this.cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ChunkPos, ChunkBlockDistro> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    private static synchronized ChunkAnalyzer getInstance(Level level) {
        return analyzers.computeIfAbsent(level, ChunkAnalyzer::new);
    }

    //**** STATIC API ****//

    public static ChunkBlockDistro blockDistribution(Level level, ChunkPos pos) {
        if (level == null || pos == null) return null;
        return getInstance(level).getDistro(pos);
    }

    public static ChunkBlockDistro areaBlockDistro(Level level, ChunkPos pos, int radius) {
        if (level == null || pos == null || radius < 0) return null;
        ChunkAnalyzer analyzer = getInstance(level);

        ChunkBlockDistro area = new ChunkBlockDistro(level, pos);
        ChunkBlockDistro center = analyzer.getDistro(pos);
        if (center != null) area.biome = center.biome;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkBlockDistro d = analyzer.getDistro(new ChunkPos(pos.x + dx, pos.z + dz));
                if (d == null) continue;
                area.merge(d);
            }
        }
        return area.chunkCount == 0 ? null : area;
    }

    //**** INSTANCE ****//

    private synchronized ChunkBlockDistro getDistro(ChunkPos pos) {
        ChunkBlockDistro cached = cache.get(pos);
        if (cached != null) return cached;

        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return null;

        ChunkBlockDistro distro = parseChunk(chunk);
        cache.put(pos, distro);
        return distro;
    }

    private ChunkBlockDistro parseChunk(LevelChunk chunk) {
        ChunkBlockDistro distro = new ChunkBlockDistro(level, chunk.getPos());
        Map<Block, Integer> counts = distro.counts;

        LevelChunkSection[] sections = chunk.getSections();
        boolean foundSurfaceSection = false;
        for (int i = sections.length - 1; i >= 0; i--) {
            LevelChunkSection section = sections[i];
            distro.total += 4096;
            if (section == null || section.hasOnlyAir()) {
                counts.merge(Blocks.AIR, 4096, Integer::sum);
                distro.airTotal += 4096;
                if (foundSurfaceSection) distro.caveAir += 4096;
                else distro.skyAir += 4096;
                continue;
            }

            boolean isSurfaceSection = !foundSurfaceSection;
            foundSurfaceSection = true;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        counts.merge(state.getBlock(), 1, Integer::sum);
                        if (!state.isAir()) continue;
                        distro.airTotal++;
                        if (isSurfaceSection) distro.skyAir++;
                        else distro.caveAir++;
                    }
                }
            }
        }
        distro.chunkCount = 1;
        distro.biome = findBiome(chunk);
        distro.structures.addAll(findStructures(chunk.getPos()));
        return distro;
    }

    private ResourceLocation findBiome(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, 8, 8);
        return HBUtil.LevelUtil.toBiomeResourceLocation(level.getBiome(new BlockPos(pos.getMiddleBlockX(), surfaceY, pos.getMiddleBlockZ())));
    }

    /**
     * Names of structures whose origin lies within STRUCTURE_SEARCH_BLOCKS horizontally of the chunk center, any height
     */
    private List<String> findStructures(ChunkPos pos) {
        StructureManager manager = StructureManager.get(level);
        if (manager == null) return List.of();
        int cx = pos.getMiddleBlockX();
        int cz = pos.getMiddleBlockZ();
        long maxDistSq = (long) STRUCTURE_SEARCH_BLOCKS * STRUCTURE_SEARCH_BLOCKS;

        List<String> names = new ArrayList<>();
        for (StructureInfo info : manager.getStructures().values()) {
            BlockPos origin = info.getOrigin();
            if (origin == null) continue;
            long dx = origin.getX() - cx;
            long dz = origin.getZ() - cz;
            if (dx * dx + dz * dz > maxDistSq) continue;
            ResourceLocation loc = info.getCurrentStructureLocation();
            if (loc != null) names.add(loc.toString());
        }
        return names;
    }

    //**** LOGGING ****//

    public static void log(ChunkBlockDistro distro, int radius, int limit) {
        if (distro == null) return;
        LoggerBase.logInfo(null, CLASS_ID + "001", format(distro, radius, limit));
    }

    /**
     * @param limit max block rows, -1 for all; air and ore rows follow the existing chunkAnalyze format
     */
    public static String format(ChunkBlockDistro distro, int radius, int limit)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Block counts for chunk ").append(distro.pos.x).append(", ").append(distro.pos.z);
        if (radius > 0) sb.append(" (radius ").append(radius).append(", ").append(distro.chunkCount).append(" loaded chunks)");
        sb.append(":");
        sb.append("\n  - Biome: ").append(distro.biome == null ? "unknown" : distro.biome);
        sb.append("\n  - Structures: ").append(Arrays.toString(distro.getStructureNames()));

        appendRow(sb, distro, "Air", distro.airTotal);
        appendRow(sb, distro, "Sky Air", distro.skyAir);
        appendRow(sb, distro, "Cave Air", distro.caveAir);

        List<Map.Entry<Block, Integer>> sorted = distro.sortedCounts();
        int shown = (limit < 0) ? sorted.size() : Math.min(limit, sorted.size());
        for (Map.Entry<Block, Integer> e : sorted.subList(0, shown)) {
            Block block = e.getKey();
            appendRow(sb, distro, HBUtil.BlockUtil.blockToString(block), e.getValue());
            if (distro.isOre(block)) {
                String base = DEEPSLATE_ORE_BLOCKS.contains(block) ? "deepslate" : "stone";
                sb.append(", ").append(String.format("%.5f", distro.oreNormalizedPercent(block))).append("% of ").append(base);
            }
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, ChunkBlockDistro distro, String name, int count) {
        sb.append("\n  - ").append(name)
            .append(" -> ").append(String.format("%,d", count))
            .append(", ").append(String.format("%,.2f", distro.perChunk(count)))
            .append(", ").append(String.format("%.5f", distro.percent(count))).append("%");
    }

    //**** INNER CLASS ****//

    public static class ChunkBlockDistro {
        public final Level level;
        public final ChunkPos pos;
        public final Map<Block, Integer> counts = new HashMap<>();
        public int total;
        public int airTotal;
        public int skyAir;
        public int caveAir;
        public int chunkCount;
        public ResourceLocation biome;
        public final Set<String> structures = new LinkedHashSet<>();

        ChunkBlockDistro(Level level, ChunkPos pos) {
            this.level = level;
            this.pos = pos;
        }

        void merge(ChunkBlockDistro other) {
            other.counts.forEach((b, c) -> counts.merge(b, c, Integer::sum));
            total += other.total;
            airTotal += other.airTotal;
            skyAir += other.skyAir;
            caveAir += other.caveAir;
            chunkCount += other.chunkCount;
            structures.addAll(other.structures);
        }

        public ResourceLocation getBiome() {
            return biome;
        }

        public String[] getStructureNames() {
            return structures.toArray(new String[0]);
        }

        public List<Map.Entry<Block, Integer>> sortedCounts() {
            List<Map.Entry<Block, Integer>> list = new ArrayList<>(counts.entrySet());
            list.sort(Map.Entry.<Block, Integer>comparingByValue().reversed());
            return list;
        }

        public double percent(Block block) {
            return percent(counts.getOrDefault(block, 0));
        }

        public double percent(int count) {
            if (total == 0) return 0;
            return 100.0 * count / total;
        }

        public boolean isOre(Block block) {
            return ORE_BLOCKS.contains(block);
        }

        public double oreNormalizedPercent(Block ore) {
            Block base = DEEPSLATE_ORE_BLOCKS.contains(ore) ? Blocks.DEEPSLATE : Blocks.STONE;
            int baseCount = counts.getOrDefault(base, 0);
            if (baseCount == 0) return 0;
            return 100.0 * counts.getOrDefault(ore, 0) / baseCount;
        }

        public double perChunk(int count) {
            if (chunkCount == 0) return 0;
            return (double) count / chunkCount;
        }
    }

}
//END CLASS CHUNKANALYZER
