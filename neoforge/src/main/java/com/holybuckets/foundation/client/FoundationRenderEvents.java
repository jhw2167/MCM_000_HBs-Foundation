package com.holybuckets.foundation.client;

import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class FoundationRenderEvents {

    private FoundationRenderEvents() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_SKY, e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterOpaqueBlocks e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_SOLID_BLOCKS, e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS, e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentParticles e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_PARTICLES, e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterWeather e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_WEATHER, e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterLevel e) ->
            fire(RenderLevelEvent.RenderStage.AFTER_LEVEL, e));
    }

    private static void fire(RenderLevelEvent.RenderStage stage, RenderLevelStageEvent event) {
        ClientEventRegistrar registrar = ClientEventRegistrar.getInstance();
        if (registrar == null) return;

        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        // Camera is no longer on the event; pull the live main camera. Projection matrix is not exposed
        // by the event (only model-view) and is unused by our consumers, so pass null.
        registrar.onRenderLevel(stage, partialTick, false,
            mc.gameRenderer.getMainCamera(), event.getPoseStack(), null);
    }
}
