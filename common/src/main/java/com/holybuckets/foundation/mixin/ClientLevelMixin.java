package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    private int clientTickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ClientLevel clientLevel = (ClientLevel) (Object) this;
        ManagedChunkEvents.onWorldTickStart(clientLevel);
    }

 /*
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerTickEnd(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        System.out.println("ServerLevel tick finished!");
    }
    */
}