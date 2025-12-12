// Common module: com/holybuckets/foundation/mixin/LevelRendererMixin.java
package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.RenderLevelCallback;
import com.holybuckets.foundation.event.RenderLevelEvents;
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

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            shift = At.Shift.AFTER
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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_SKY,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_SOLID_BLOCKS,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            ordinal = 2,
            shift = At.Shift.AFTER
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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_TRANSLUCENT_BLOCKS,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
            shift = At.Shift.AFTER
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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_PARTICLES,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
            shift = At.Shift.AFTER
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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_WEATHER,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }

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
        RenderLevelEvents.fire(
            RenderLevelCallback.RenderStage.AFTER_LEVEL,
            poseStack, partialTick, finishNanoTime, renderBlockOutline,
            camera, gameRenderer, lightTexture, projectionMatrix
        );
    }
}