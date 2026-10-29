package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Silences "Failed to store chunk" when the region file channel was already closed by another
 * thread (seen during Chunk Pregenerator runs). All other store failures still log as errors.
 */
@Mixin(IOWorker.class)
public abstract class IOWorkerMixin {

    @Unique private static final AtomicLong hbs$suppressedClosedChannelErrors = new AtomicLong();

    @Redirect(
        method = "runStore",
        at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V")
    )
    private void hbs$onStoreFailedLog(Logger logger, String message, Object chunkPos, Object error) {
        if (error instanceof ClosedChannelException) {
            long count = hbs$suppressedClosedChannelErrors.incrementAndGet();
            LoggerBase.logDebug(null, "IOWORKER_CLOSED_CHANNEL",
                "Suppressed ClosedChannelException storing chunk " + chunkPos + " (total " + count + ")");
            return;
        }
        logger.error(message, chunkPos, error);
    }

}
