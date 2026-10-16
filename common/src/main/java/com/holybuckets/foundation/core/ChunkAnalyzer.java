package com.holybuckets.foundation.core;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

public class ChunkAnalyzer {

    public static final String CLASS_ID = "041";
    public static final int MAX_CACHE_SIZE = 256;

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

        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir()) {
                counts.merge(Blocks.AIR, 4096, Integer::sum);
                distro.total += 4096;
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        counts.merge(section.getBlockState(x, y, z).getBlock(), 1, Integer::sum);
                    }
                }
            }
            distro.total += 4096;
        }
        distro.chunkCount = 1;
        return distro;
    }

    //**** INNER CLASS ****//

    public static class ChunkBlockDistro {
        public final Level level;
        public final ChunkPos pos;
        public final Map<Block, Integer> counts = new HashMap<>();
        public int total;
        public int chunkCount;

        ChunkBlockDistro(Level level, ChunkPos pos) {
            this.level = level;
            this.pos = pos;
        }

        void merge(ChunkBlockDistro other) {
            other.counts.forEach((b, c) -> counts.merge(b, c, Integer::sum));
            total += other.total;
            chunkCount += other.chunkCount;
        }

        public List<Map.Entry<Block, Integer>> sortedCounts() {
            List<Map.Entry<Block, Integer>> list = new ArrayList<>(counts.entrySet());
            list.sort(Map.Entry.<Block, Integer>comparingByValue().reversed());
            return list;
        }

        public double percent(Block block) {
            if (total == 0) return 0;
            return 100.0 * counts.getOrDefault(block, 0) / total;
        }
    }

}
//END CLASS CHUNKANALYZER
