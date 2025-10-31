package com.holybuckets.foundation.client;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.structure.StructureManager;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.client.BlockHighlightDrawEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.client.Minecraft;

public class CommonClassClient {


    public static void initClient() {
        initClientEvents();
        initRenderers();
    }

    //** CLIENT INITIALIZERS **//
    private static void initClientEvents() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        //reg.registerOnServerStop(CommonClassClient::onServerStop, EventPriority.Lowest);
        //reg.registerOnBlockHighlightDraw(CommonClassClient::onBlockHighlightDraw, EventPriority.Normal);
        ClientInput.init(reg);
        initStructureManager(reg);

        ClientBalmEventRegister.registerEvents();
    }

    public static void initStructureManager(ClientEventRegistrar reg) {
        reg.registerOnClientTick(TickType.ON_120_TICKS ,
             e -> StructureManager.fireSyncClientStructureCountsToServer(Minecraft.getInstance().player));
        reg.registerOnConnectedToServer(
             e -> StructureManager.onConnectedToServer(Minecraft.getInstance().player));
        StructureManager.clientInit();
    }

    private static void initRenderers() {
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }

    private static void onServerStop(ServerStoppedEvent event) {
        //DataStore.removeClientWorldSaveData(Minecraft.getInstance().getLevelSource());
    }

    private static void onBlockHighlightDraw(BlockHighlightDrawEvent event) {
        event.setCanceled(true);
    }

}
