package com.holybuckets.foundation.client;

import com.holybuckets.foundation.FoundationInitializers;
import com.holybuckets.foundation.event.EventRegistrar;

import static com.holybuckets.foundation.FoundationInitializers.initRenderers;

public class CommonClassClient {


    public static void initClient() {
        initRenderers();
        EventRegistrar reg = EventRegistrar.getInstance();
        com.holybuckets.foundation.client.ClientInput.init(reg);
    }
}
