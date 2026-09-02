package com.holybuckets.foundation.client;
import com.holybuckets.foundation.CommonClass;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.core.MovingWaypoint;
import com.holybuckets.foundation.core.WoolColorHelper;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ClientTickEvent;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.structure.StructureManager;
import net.blay09.mods.balm.client.BalmClientRegistrars;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.client.BlockHighlightDrawEvent;
import com.holybuckets.foundation.event.balm.client.ConnectedToServerEvent;
import com.holybuckets.foundation.event.balm.server.ServerStoppedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class CommonClassClient {


    public static void initClient(BalmClientRegistrars registrars) {
        //testRenderers();
        //testScreenDraw();
        initRenderers(registrars);
        initClientEvents();
    }

    //** CLIENT INITIALIZERS **//
    private static void initClientEvents() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        MessagerClient messager = MessagerClient.getInstance();
        reg.registerOnConnectedToServer(CommonClassClient::onPlayerConnectToServer, EventPriority.Highest);
        reg.registerOnClientTick(TickType.ON_SINGLE_TICK, CommonClassClient::onClientTick, EventPriority.Lowest);
        //reg.registerOnServerStop(CommonClassClient::onServerStop, EventPriority.Lowest);
        //reg.registerOnBlockHighlightDraw(CommonClassClient::onBlockHighlightDraw, EventPriority.Normal);
        ClientInput.init(reg);
        messager.init(reg);
        initStructureManager(reg);
        MovingWaypoint.registerEvents(reg);


        ClientBalmEventRegister.registerEvents();
    }


    public static void initStructureManager(ClientEventRegistrar reg) {
        reg.registerOnClientTick(TickType.ON_120_TICKS ,
             e -> StructureManager.fireSyncClientStructureCountsToServer(Minecraft.getInstance().player));
    }

    private static void initRenderers(BalmClientRegistrars registrars) {
        registrars.blockEntityRenderers(ModRenderers::clientInitialize);
    }

    //** Events
    private static void onPlayerConnectToServer(ConnectedToServerEvent event)
    {
        CommonClass.MESSAGER = MessagerClient.getInstance();
        boolean isServerSide = GeneralConfig.getInstance().isServerSide();
        Player player = Minecraft.getInstance().player;
        ManagedPlayer.onClientConnectedToServer(player);

        if(isServerSide) return;
        EventRegistrar.onPlayerConnectedToServer();
        GeneralConfig.getInstance().onPlayerConnectedToServer(player);
        DataStore.onPlayerConnectToServer(getServerName(event.getClient()));
        WoolColorHelper.initWoolColors();
    }

        private static String getServerName(Minecraft mc)
        {
            if (mc.getCurrentServer() == null || mc.getCurrentServer().name == null) {
                return "Unknown Server";
            }
            String serverName = mc.getCurrentServer().name;
            String serverIp = mc.getCurrentServer().ip;
            //Combine serverName and last 4 digits of serverIp for a unique identifier
            String serverIdentifier = serverName + "_" + (serverIp.length() > 4 ? serverIp.substring(serverIp.length() - 4) : serverIp);
            return serverIdentifier;
        }


    private static void onClientTick(ClientTickEvent event) {
        ManagedPlayer.onClientTick(Minecraft.getInstance().player);
    }

    //** Tests


    private static void onServerStop(ServerStoppedEvent event) {
        //DataStore.removeClientWorldSaveData(Minecraft.getInstance().getLevelSource());
    }

    private static void onBlockHighlightDraw(BlockHighlightDrawEvent event) {
        event.setCanceled(true);
    }


    private static void testScreenDraw()
    {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();

        reg.registerOnScreenDrawPre( event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "SCREEN_DRAW_TEST", "ScreenDrawEvent PRE fired - mouseX: " + event.getMouseX() + ", mouseY: " + event.getMouseY());
            }
        });

        reg.registerOnScreenDrawPost( event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "SCREEN_DRAW_TEST", "ScreenDrawEvent POST fired - screen width: " + event.getScreen().width + ", screen height: " + event.getScreen().height);
            }
        });
    }

    private static void testRenderers()
    {
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
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_TRANSLUCENT_BLOCKS fired - camera position: " + event.getCamera().position());
            }
        });
        
        // Test AFTER_PARTICLES stage
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, event -> {
            if (Math.random() < 0.1) {
                LoggerBase.logDebug(null, "RENDER_TEST", "RenderLevelEvent AFTER_PARTICLES fired - partialTick: " + event.getPartialTick());
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

}
