package com.holybuckets.foundation.event.custom;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
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
    private DeltaTracker deltaTracker;
    private boolean renderBlockOutline;
    private Camera camera;
    private GameRenderer gameRenderer;
    private LightTexture lightTexture;
    private Matrix4f modelViewMatrix;
    private Matrix4f projectionMatrix;

    public RenderLevelEvent(RenderStage stage, DeltaTracker deltaTracker,
                           boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                           LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        this.stage = stage;
        this.deltaTracker = deltaTracker;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.lightTexture = lightTexture;
        this.modelViewMatrix = modelViewMatrix;
        this.projectionMatrix = projectionMatrix;
    }

    // Package-private constructor for static instance
    public RenderLevelEvent() {
    }

    // Package-private method to update values
    public void updateValues(RenderStage stage, DeltaTracker deltaTracker,
                             boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                             LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        this.stage = stage;
        this.deltaTracker = deltaTracker;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.lightTexture = lightTexture;
        this.modelViewMatrix = modelViewMatrix;
        this.projectionMatrix = projectionMatrix;
    }

    public RenderStage getStage() {
        return stage;
    }

    public DeltaTracker getDeltaTracker() {
        return deltaTracker;
    }

    /**
     * Convenience method — extracts partial tick from DeltaTracker.
     * Preserves the same API for consumers that previously called getPartialTick().
     */
    public float getPartialTick() {
        return deltaTracker.getGameTimeDeltaPartialTick(false);
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

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
