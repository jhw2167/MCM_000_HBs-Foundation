package com.holybuckets.foundation;

import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.client.CommonClassClient;
import com.holybuckets.foundation.client.render.BlockEntityItemRenderer;
import com.holybuckets.foundation.client.render.ModelItemRenderer;
import com.holybuckets.foundation.event.custom.RegisterCustomBlockEntitiesEvent;
import com.holybuckets.foundation.event.custom.RegisterCustomItemModelsEvent;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.ItemLike;


public class FoundationMainClientFabric implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerItemRenderers());
    }

    private static void registerItemRenderers() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();

        for (RegisterCustomBlockEntitiesEvent.Entry entry : reg.onRegisterCustomBlockEntities().getEntries()) {
            setItemRenderer(entry.item().get(), new BlockEntityItemRenderer(entry.type(), entry.state()));
        }

        for (RegisterCustomItemModelsEvent.Entry entry : reg.onRegisterCustomItemModels().getEntries()) {
            setItemRenderer(entry.item().get(),
                new ModelItemRenderer(entry.layer(), entry.texture(), entry.renderType()));
        }
    }

    private static void setItemRenderer(ItemLike item, BlockEntityWithoutLevelRenderer renderer) {
        try {
            BuiltinItemRendererRegistry.INSTANCE.register(item, renderer::renderByItem);
        } catch (IllegalArgumentException e) {
            LoggerBase.logWarning(null, "010100", "Item " + item.asItem() + " already has a builtin item renderer, skipping custom item renderer");
        }
    }

}
