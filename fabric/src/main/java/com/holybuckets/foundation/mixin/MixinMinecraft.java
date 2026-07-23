package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.balm.LevelLoadingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    public ClientLevel level;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CallbackInfo info) {
    }

    @Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    public void clearClientLevel(Screen p_91321_, CallbackInfo ci) {
        if (this.level != null && EventRegistrar.getInstance() != null) {
            EventRegistrar.getInstance().onLevelUnload(new LevelLoadingEvent.Unload(this.level));
        }
    }

    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)V", at = @At("TAIL"))
    public void setLevel(ClientLevel clientLevel, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (clientLevel != null && EventRegistrar.getInstance() != null) {
            EventRegistrar.getInstance().onLevelLoad(new LevelLoadingEvent.Load(clientLevel));
        }
    }
}
