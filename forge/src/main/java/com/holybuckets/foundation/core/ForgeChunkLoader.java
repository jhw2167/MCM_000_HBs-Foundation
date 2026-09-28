package com.holybuckets.foundation.core;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.platform.services.ChunkLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import pregenerator.common.base.ListenerStorage;
import pregenerator.common.base.ProcessListener;
import pregenerator.common.base.TaskStorage;
import pregenerator.common.generator.GenerationType;
import pregenerator.common.generator.tasks.SquareAreaTask;
import pregenerator.common.manager.ServerManager;

import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.holybuckets.foundation.HBUtil.ChunkUtil.MAX_CHUNK_VALUE;

public class ForgeChunkLoader implements ChunkLoader {

    public static final String CLASS_ID = "039";
    private static final String ID = "foundations:ChunkExplorer";
    private static final int GEN = GenerationType.NORMAL_GEN.getIndex();

    // One entry per level with a pregen task in flight. Only one level may hold a task at a
    // time for now, so these are empty or hold exactly one entry.
    private final Map<ResourceKey<Level>, UUID> current = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, ChunkPos> currentPos = new ConcurrentHashMap<>();
    private final Set<UUID> suppressedListeners = new LinkedHashSet<>();
    private boolean suppressed = false;

    final static int radius = 1;
    @Override
    public boolean forceChunkLoad(ServerLevel level, ChunkPos pos) {
        if (!current.isEmpty()) return false;

        if (Math.abs(pos.x) > MAX_CHUNK_VALUE || Math.abs(pos.z) > MAX_CHUNK_VALUE) {
            LoggerBase.logError(null, "039000", "Refusing out of range chunk " + pos);
            return false;
        }

        UUID id = UUID.nameUUIDFromBytes((ID + pos.x + "_" + pos.z).getBytes(StandardCharsets.UTF_8));
        ChunkPos max = new ChunkPos(pos.x + radius, pos.z + radius);
        ChunkPos min = new ChunkPos(pos.x - radius, pos.z - radius);
        SquareAreaTask task = new SquareAreaTask(id.toString(), level.dimension(), GEN, min, max);
        current.put(level.dimension(), id);
        currentPos.put(level.dimension(), pos);
        ServerManager.INSTANCE.startTask(task, id, this::logUpdate);
        suppressListeners();
        return true;
    }


    @Override
    public boolean unforceChunkLoad(ServerLevel level, ChunkPos pos) {
        if (current.isEmpty()) return true;

        // Another dimension is running
        ResourceKey<Level> dim = level.dimension();
        if (!current.containsKey(dim)) return false;

        if (ServerManager.INSTANCE.isRunning(dim)) {
            suppressListeners();
            return false;
        }

        current.remove(dim);
        currentPos.remove(dim);
        if (current.isEmpty()) restoreListeners();
        return true;
    }

    private void suppressListeners() {
        if (suppressed) return;
        suppressed = true;

        ListenerStorage storage = TaskStorage.getListeners();

        // The console listener has a null owner and routes to MinecraftServer.sendSystemMessage,
        // which is the recurring [Dim=...] status block in the server log.
        suppress(storage, null);
        suppress(storage, ProcessListener.SERVER.getOwner());

        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            suppress(storage, player.getUUID());
        }
        for (UUID owner : current.values()) {
            ServerManager.INSTANCE.removeListener(owner);
        }
    }

    private void suppress(ListenerStorage storage, UUID id) {
        try {
            if (!storage.isAutoListening(id)) return;
            storage.add(id, false);
            ServerManager.INSTANCE.removeListener(id);
            suppressedListeners.add(id);
        } catch (Throwable t) {
            LoggerBase.logWarning(null, "039001", "Could not mute pregen listener " + id + ": " + t);
        }
    }

    @Override
    public void restoreListeners() {
        ListenerStorage storage = TaskStorage.getListeners();
        for (UUID id : suppressedListeners) {
            try {
                storage.add(id, true);
                ServerManager.INSTANCE.addListener(id);
            } catch (Throwable t) {
                LoggerBase.logWarning(null, "039002", "Could not restore pregen listener " + id + ": " + t);
            }
        }
        suppressedListeners.clear();
        suppressed = false;
    }

    public boolean isRunningOnLevel(ServerLevel level) {
        return ServerManager.INSTANCE.getData(level.dimension())!=null;
    }

    @Override
    public boolean isRunning(ServerLevel level) {
        if (current.isEmpty()) return false;
        return ServerManager.INSTANCE.isRunning(level.dimension());
    }


    private void logUpdate(Component n) {
        String id = "null";
        for (Map.Entry<ResourceKey<Level>, ChunkPos> e : currentPos.entrySet()) {
            id = e.getKey().location() + " " + HBUtil.ChunkUtil.getId(e.getValue());
            break;
        }
        LoggerBase.logInfo(null, "039002", "Chunk Explorer: " + id + " - " + n.getString());
    }
}
