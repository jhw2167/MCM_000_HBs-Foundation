package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.core.ChunkExplorerManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chunk Pregenerator reports task progress by pushing a Component to every registered
 * ProcessListener. The console listener has a null owner and writes straight to the server
 * log, so background explorer loads spam it. Swallow the message while the explorer owns
 * the current interval.
 *
 * Targets another mod, so remap is off and the config that carries this mixin is optional.
 */
@Mixin(targets = "pregenerator.common.base.ProcessListener", remap = false)
public class MixinProcessListener {

    @Inject(method = "sendMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void hbs$suppressExplorerMessages(Component component, CallbackInfo ci) {
        if (ChunkExplorerManager.checkExploreInterval()) ci.cancel();
    }
}
