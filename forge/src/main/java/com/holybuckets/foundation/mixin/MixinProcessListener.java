package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.core.ChunkExplorerManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "pregenerator.common.base.ProcessListener", remap = false)
public class MixinProcessListener {

    @Inject(method = "sendMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void hbs$suppressExplorerMessages(Component component, CallbackInfo ci) {
        if (ChunkExplorerManager.checkExploreInterval()) ci.cancel();
    }
}
