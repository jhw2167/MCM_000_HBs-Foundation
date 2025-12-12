package com.holybuckets.foundation.event.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

public class RenderLevelEvent {
    
    public enum RenderStage {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL
    }
    
    private RenderStage stage;
    private PoseStack poseStack;
    private float partialTick;
    private long finishNanoTime;
    private boolean renderBlockOutline;
    private Camera camera;
    private GameRenderer gameRenderer;
    private LightTexture lightTexture;
    private Matrix4f projectionMatrix;
    
    public RenderLevelEvent(RenderStage stage, PoseStack poseStack, float partialTick, long finishNanoTime, 
                           boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, 
                           LightTexture lightTexture, Matrix4f projectionMatrix) {
        this.stage = stage;
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        this.finishNanoTime = finishNanoTime;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.lightTexture = lightTexture;
        this.projectionMatrix = projectionMatrix;
    }
    
    // Package-private constructor for static instance
    public RenderLevelEvent() {
    }
    
    // Package-private method to update values
    public void updateValues(RenderStage stage, PoseStack poseStack, float partialTick, long finishNanoTime,
                             boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                             LightTexture lightTexture, Matrix4f projectionMatrix) {
        this.stage = stage;
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        this.finishNanoTime = finishNanoTime;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.lightTexture = lightTexture;
        this.projectionMatrix = projectionMatrix;
    }
    
    public RenderStage getStage() {
        return stage;
    }
    
    public PoseStack getPoseStack() {
        return poseStack;
    }
    
    public float getPartialTick() {
        return partialTick;
    }
    
    public long getFinishNanoTime() {
        return finishNanoTime;
    }
    
    public boolean isRenderBlockOutline() {
        return renderBlockOutline;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public GameRenderer getGameRenderer() {
        return gameRenderer;
    }
    
    public LightTexture getLightTexture() {
        return lightTexture;
    }
    
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
