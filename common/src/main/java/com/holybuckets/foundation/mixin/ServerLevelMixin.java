package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.util.MixinManager;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {


    /*
    private int serverTickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onServerTickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if(MixinManager.isEnabled("ServerLevelMixin::onServerTickStart")) {
            try {
                ServerLevel level = (ServerLevel) (Object) this;
                ManagedChunkEvents.onWorldTickStart(level);
            }
            catch (Exception e) {
                MixinManager.recordError("ServerLevelMixin::onServerTickStart", e);
            }

        }

    }*/

    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
    private void onWakeUpAllPlayers(CallbackInfo ci) {
        if (MixinManager.isEnabled("ServerLevelMixin::onWakeUpAllPlayers")) {
            try {
                ServerLevel level = (ServerLevel) (Object) this;
                // Your custom handler here
                EventRegistrar.getInstance().onWakeUpAllPlayers(level);
            }
            catch (Exception e) {
                MixinManager.recordError("ServerLevelMixin::onWakeUpAllPlayers", e);
            }
        }
    }

    /*
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerTickEnd(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        System.out.println("ServerLevel tick finished!");
    }
    */

}
