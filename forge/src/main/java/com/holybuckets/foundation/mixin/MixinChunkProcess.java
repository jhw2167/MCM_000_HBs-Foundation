package com.holybuckets.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "pregenerator.common.generator.ChunkProcess", remap = false)
public class MixinChunkProcess {

    @Inject(method = "clearFiles()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbs$keepRegionFilesOpen(CallbackInfo ci) {
        ci.cancel();
    }
}
