package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.util.MixinManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CallbackInfo info) {
        //Constants.LOG.info("MIXIN MINECRAFT SERVER CONSTRUCTOR!");
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    private void onTickChildren(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if(MixinManager.isEnabled("MinecraftServerMixin::onTickChildren")) {
            try {
                //EventRegistrar.getInstance().onServerTick(server);
            } catch (Exception e) {
                MixinManager.recordError("MinecraftServerMixin::onTickChildren", e);
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
