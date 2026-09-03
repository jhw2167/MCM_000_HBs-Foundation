package com.holybuckets.foundation.core;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.platform.services.ChunkLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import pregenerator.common.generator.GenerationType;
import pregenerator.common.generator.tasks.SquareAreaTask;
import pregenerator.common.manager.ServerManager;

import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static com.holybuckets.foundation.HBUtil.ChunkUtil.MAX_CHUNK_VALUE;

public class ForgeChunkLoader implements ChunkLoader {

    public static final String CLASS_ID = "039";
    private static final String ID = "foundations:ChunkExplorer";
    private static final int GEN = GenerationType.NORMAL_GEN.getIndex();

    private UUID current;
    private final Set<UUID> suppressedListeners = new LinkedHashSet<>();

    @Override
    public boolean forceChunkLoad(ServerLevel level, ChunkPos pos) {
        if (current != null) return false;

        if (Math.abs(pos.x) > MAX_CHUNK_VALUE || Math.abs(pos.z) > MAX_CHUNK_VALUE) {
            LoggerBase.logError(null, CLASS_ID, "Refusing out of range chunk " + pos);
            return false;
        }
        int radius = 1;
        UUID id = UUID.nameUUIDFromBytes((ID + pos.x + "_" + pos.z).getBytes(StandardCharsets.UTF_8));
        ChunkPos max = new ChunkPos(pos.x + radius, pos.z + radius);
        ChunkPos min = new ChunkPos(pos.x - radius, pos.z - radius);
        SquareAreaTask task = new SquareAreaTask(id.toString(), level.dimension(), GEN, min, max);
        ServerManager.INSTANCE.startTask(task, id, this::logUpdate);
        current = id;
        suppressListeners();
        return true;
    }

    /**
     * dont report background chunk  loads to players
     */
    private void suppressListeners() {
        suppressedListeners.clear();
        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            UUID playerId = player.getUUID();
            if (!ServerManager.INSTANCE.isListening(playerId)) continue;
            ServerManager.INSTANCE.removeListener(playerId);
            suppressedListeners.add(playerId);
        }
        ServerManager.INSTANCE.removeListener(current);
    }

    private void restoreListeners() {
        for (UUID playerId : suppressedListeners) {
            ServerManager.INSTANCE.addListener(playerId);
        }
        suppressedListeners.clear();
    }

    @Override
    public boolean unforceChunkLoad(ServerLevel level, ChunkPos pos) {
        if (current == null) return true;
        if (ServerManager.INSTANCE.isRunning(level.dimension())) {
            suppressListeners();
            return false;
        }
        current = null;
        restoreListeners();
        return true;
    }

    private void logUpdate(Component n) {
        String id = (current == null) ? "null" : current.toString();
        LoggerBase.logInfo(null, CLASS_ID, "Chunk Explorer: " + id + " - " + n.getString());
    }
}
