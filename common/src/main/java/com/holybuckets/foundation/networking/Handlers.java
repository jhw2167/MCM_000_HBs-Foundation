package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.ThreadPoolExecutor;

public class Handlers {

    public static String CLASS_ID = "014";

    private static int RECEIVED = 0;
    private static ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());

    public static void handleBlockStateUpdates(Player p, BlockStateUpdatesMessage m) {
        RECEIVED++;
        POOL.submit(() -> BlockStateUpdatesMessageHandler.handle(p, m));
    }


}
