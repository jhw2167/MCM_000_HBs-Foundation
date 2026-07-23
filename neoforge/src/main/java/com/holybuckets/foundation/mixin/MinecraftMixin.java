package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.balm.client.ClientStartedEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "run()V", at = @At("HEAD"))
    void run(CallbackInfo callbackInfo) {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        if (reg != null) {
            reg.onClientStarted(new ClientStartedEvent(Minecraft.getInstance()));
        }
    }
}
