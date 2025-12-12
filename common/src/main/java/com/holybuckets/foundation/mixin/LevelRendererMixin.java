package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
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
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_SKY,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
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
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_SOLID_BLOCKS,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
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
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
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
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    // After weather
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V",
            shift = At.Shift.BEFORE
        )
    )
    private void afterWeather(
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_WEATHER,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    // At the end of renderLevel
    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
    private void afterLevel(
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        ClientEventRegistrar.getInstance().onRenderLevel(RenderLevelEvent.RenderStage.AFTER_LEVEL,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }
}