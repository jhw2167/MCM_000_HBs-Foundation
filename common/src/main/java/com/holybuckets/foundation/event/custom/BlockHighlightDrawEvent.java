package com.holybuckets.foundation.event.custom;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

public class BlockHighlightDrawEvent {
    private final LevelRenderer levelRenderer;
    private final Camera camera;
    private final BlockHitResult target;
    private final PoseStack poseStack;
    private final float partialTicks;

    public BlockHighlightDrawEvent(LevelRenderer levelRenderer, Camera camera, BlockHitResult target, PoseStack poseStack, float partialTicks) {
        this.levelRenderer = levelRenderer;
        this.camera = camera;
        this.target = target;
        this.poseStack = poseStack;
        this.partialTicks = partialTicks;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public Camera getCamera() {
        return camera;
    }

    public BlockHitResult getTarget() {
        return target;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
