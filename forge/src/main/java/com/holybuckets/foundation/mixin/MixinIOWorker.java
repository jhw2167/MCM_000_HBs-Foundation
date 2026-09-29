package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.CommonClass;
import com.holybuckets.foundation.GeneralConfig;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.channels.ClosedChannelException;

@Mixin(IOWorker.class)
public class MixinIOWorker {

    @Redirect(method = "runStore",
        at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"),
        require = 1)
    private void hbs$dropClosedChannelReport(Logger logger, String message, Object pos, Object thrown) {
        if (thrown instanceof ClosedChannelException && hbs$explorerActive()) return;
        logger.error(message, pos, thrown);
    }

    private static boolean hbs$explorerActive() {
        GeneralConfig config = GeneralConfig.getInstance();
        CommonClass.check(0);
        return config.getPerformanceImpactConfig().isEnableChunkExplorer();
    }
}
