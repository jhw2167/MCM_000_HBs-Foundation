package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // After sky rendering
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=sky"
        )
    )
    private void afterSky(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_SKY,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }

    // After solid blocks
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=entities"
        )
    )
    private void afterSolidBlocks(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_SOLID_BLOCKS,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }

    // After translucent blocks (RECOMMENDED FOR BEACON BEAMS)
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=translucent"
        )
    )
    private void afterTranslucentBlocks(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }

    // After particles
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=particles"
        )
    )
    private void afterParticles(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }

    // After weather — target changed from DebugRenderer.render() to LevelRenderer.renderDebug()
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void afterWeather(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_WEATHER,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }

    // At the end of renderLevel
    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
    private void afterLevel(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_LEVEL,
            deltaTracker, renderBlockOutline,
            camera, gameRenderer, lightTexture,
            modelViewMatrix, projectionMatrix
        );
    }
}