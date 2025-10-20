package com.holybuckets.foundation.client;
import com.holybuckets.foundation.datastore.DataStore;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.EventPriority;
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

        ClientBalmEventRegister.registerEvents();
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
