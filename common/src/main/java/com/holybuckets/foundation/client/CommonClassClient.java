package com.holybuckets.foundation.client;
import net.blay09.mods.balm.api.client.BalmClient;

public class CommonClassClient {


    public static void initClient() {
        initClientEvents();
        initRenderers();
    }

    //** CLIENT INITIALIZERS **//
    private static void initClientEvents() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();
        ClientInput.init(reg);

        ClientBalmEventRegister.registerEvents();
    }

    private static void initRenderers() {
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }


}
