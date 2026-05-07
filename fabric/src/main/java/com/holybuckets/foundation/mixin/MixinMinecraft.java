package com.holybuckets.foundation.mixin;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.blay09.mods.balm.fabric.event.FabricBalmCommonEvents;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    public ClientLevel level;
    
    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CallbackInfo info) {

        //Constants.LOG.info("This line is printed by an example mod common mixin!");
        //Constants.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }

    @Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    public void clearClientLevel(Screen p_91321_, CallbackInfo ci) {
        if (this.level != null) {
            Balm.getEvents().fireEvent(new LevelLoadingEvent.Unload(this.level));
        }
    }

    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)V", at = @At("TAIL"))
    public void setLevel(ClientLevel clientLevel, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (clientLevel != null) {
            Balm.getEvents().fireEvent(new LevelLoadingEvent.Load(clientLevel));
        }
    }
}