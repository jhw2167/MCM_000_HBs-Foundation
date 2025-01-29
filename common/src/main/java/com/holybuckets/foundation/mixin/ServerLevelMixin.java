package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    private int serverTickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onServerTickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ManagedChunkEvents.onWorldTickStart(level);
    }

    /*
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerTickEnd(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        System.out.println("ServerLevel tick finished!");
    }
    */

}
