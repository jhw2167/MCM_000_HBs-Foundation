package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 PORT NOTE: renderLevel was reworked into a frame-graph/render-state pipeline.
 * Injections now target the frame pass name constants and capture no locals; render
 * context (camera, delta) is pulled from Minecraft.getInstance(). Matrices are no
 * longer available at these injection points and are passed as null.
 * Each injection uses require = 0 so a missed target logs instead of crashing;
 * verify each stage fires in-game.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    private void hbs$fireStage(RenderLevelEvent.RenderStage stage) {
        Minecraft mc = Minecraft.getInstance();
        ClientEventRegistrar registrar = ClientEventRegistrar.getInstance();
        if (registrar == null) return;
        registrar.onRenderLevel(stage,
            mc.getDeltaTracker(), true,
            mc.gameRenderer.getMainCamera(), mc.gameRenderer, mc.gameRenderer.lightTexture(),
            null, null
        );
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=sky"), require = 0)
    private void afterSky(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_SKY);
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"), require = 0)
    private void afterSolidBlocks(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_SOLID_BLOCKS);
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"), require = 0)
    private void afterTranslucentBlocks(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS);
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=particles"), require = 0)
    private void afterParticles(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_PARTICLES);
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=weather"), require = 0)
    private void afterWeather(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_WEATHER);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"), require = 0)
    private void afterLevel(CallbackInfo ci) {
        hbs$fireStage(RenderLevelEvent.RenderStage.AFTER_LEVEL);
    }
}
