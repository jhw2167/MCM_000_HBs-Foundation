package com.holybuckets.foundation.client;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.structure.StructureManager;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.client.BlockHighlightDrawEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.client.Minecraft;

public class CommonClassClient {


    public static void initClient() {
        initClientEvents();
        initRenderers();
        //testRenderers();
    }

    //** CLIENT INITIALIZERS **//
    private static void initClientEvents() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        //reg.registerOnServerStop(CommonClassClient::onServerStop, EventPriority.Lowest);
        //reg.registerOnBlockHighlightDraw(CommonClassClient::onBlockHighlightDraw, EventPriority.Normal);
        ClientInput.init(reg);
        initManagedPlayer(reg);
        initStructureManager(reg);


        ClientBalmEventRegister.registerEvents();
    }

    public static void initManagedPlayer(ClientEventRegistrar reg) {
    reg.registerOnConnectedToServer( e -> ManagedPlayer.onClientConnectedToServer(
        Minecraft.getInstance().player), EventPriority.Highest);
    }

    public static void initStructureManager(ClientEventRegistrar reg) {
        reg.registerOnClientTick(TickType.ON_120_TICKS ,
             e -> StructureManager.fireSyncClientStructureCountsToServer(Minecraft.getInstance().player));
             /*
        reg.registerOnConnectedToServer(
             e -> StructureManager.onConnectedToServer(Minecraft.getInstance().player));
        StructureManager.clientInit();
              */
    }

    private static void initRenderers() {
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }

    private static void testRenderers() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        
        // Test AFTER_SKY stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_SKY, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_SKY fired - partialTick: " + event.getPartialTick());
            }
        });
        
        // Test AFTER_SOLID_BLOCKS stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_SOLID_BLOCKS, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_SOLID_BLOCKS fired - renderBlockOutline: " + event.isRenderBlockOutline());
            }
        });
        
        // Test AFTER_TRANSLUCENT_BLOCKS stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_TRANSLUCENT_BLOCKS fired - camera position: " + event.getCamera().getPosition());
            }
        });
        
        // Test AFTER_PARTICLES stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_PARTICLES fired - finishNanoTime: " + event.getFinishNanoTime());
            }
        });
        
        // Test AFTER_WEATHER stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_WEATHER, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_WEATHER fired - stage: " + event.getStage().name());
            }
        });
        
        // Test AFTER_LEVEL stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_LEVEL, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_LEVEL fired - projection matrix: " + event.getProjectionMatrix());
            }
        });
    }

    private static void onServerStop(ServerStoppedEvent event) {
        //DataStore.removeClientWorldSaveData(Minecraft.getInstance().getLevelSource());
    }

    private static void onBlockHighlightDraw(BlockHighlightDrawEvent event) {
        event.setCanceled(true);
    }

}
