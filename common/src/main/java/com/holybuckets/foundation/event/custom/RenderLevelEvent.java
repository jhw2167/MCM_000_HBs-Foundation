package com.holybuckets.foundation.event.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

// 26.1.2 PORT: In-world render stages are no longer sourced from a LevelRenderer mixin (the
// renderLevel pipeline was reworked and the old string-constant inject points are gone). They now
// come from the loader-native render-stage events — NeoForge RenderLevelStageEvent and Fabric
// WorldRenderEvents — which supply the live PoseStack, camera, projection matrix and partial tick.
// LightTexture / GameRenderer / DeltaTracker are no longer carried (they weren't used by consumers).
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
    private float partialTick;
    private boolean renderBlockOutline;
    private Camera camera;
    private PoseStack poseStack;
    private Matrix4f projectionMatrix;

    public RenderLevelEvent() {
    }

    public RenderLevelEvent(RenderStage stage, float partialTick, boolean renderBlockOutline,
                            Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        updateValues(stage, partialTick, renderBlockOutline, camera, poseStack, projectionMatrix);
    }

    public void updateValues(RenderStage stage, float partialTick, boolean renderBlockOutline,
                             Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        this.stage = stage;
        this.partialTick = partialTick;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.poseStack = poseStack;
        this.projectionMatrix = projectionMatrix;
    }

    public RenderStage getStage() {
        return stage;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public boolean isRenderBlockOutline() {
        return renderBlockOutline;
    }

    public Camera getCamera() {
        return camera;
    }

    /** The live world-render PoseStack from the loader render event. Use this to position draws. */
    public PoseStack getPoseStack() {
        return poseStack;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
