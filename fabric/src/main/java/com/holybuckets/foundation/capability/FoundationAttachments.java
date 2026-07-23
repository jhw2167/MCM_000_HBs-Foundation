package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.event.balm.ChunkLoadingEvent;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.blay09.mods.balm.platform.event.callback.LevelCallback;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;

public class FoundationAttachments {

    public static void init() {
        initAttachments();
        registerAttachments();
    }

    private static void initAttachments() {
        ManagedChunkAttachment.init();
        ManagedPlayerAttachment.init();
    }


    private static void registerAttachments() {
        LevelCallback.Chunk.LOAD.register((level, chunk, chunkPos) ->
            ManagedChunkAttachment.onChunkLoadRegisterAttachment(new ChunkLoadingEvent.Load(level, chunk, chunkPos)));
        ServerPlayerCallback.Join.EVENT.register(EventPriority.Highest.toPhase(), player ->
            ManagedPlayerAttachment.onPlayerLoginRegisterAttachment(new PlayerLoginEvent(player)));
    }


}
