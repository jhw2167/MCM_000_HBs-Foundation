package com.holybuckets.foundation.event;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.client.render.BlockEntityItemRenderer;
import com.holybuckets.foundation.client.render.ModelItemRenderer;
import com.holybuckets.foundation.event.custom.RegisterCustomBlockEntitiesEvent;
import com.holybuckets.foundation.event.custom.RegisterCustomItemModelsEvent;
import com.holybuckets.foundation.mixin.ItemRenderPropertiesAccessor;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ForgeClientModEvents::registerItemRenderers);
    }

    private static void registerItemRenderers() {
        ClientEventRegistrar reg = ClientEventRegistrar.getInstance();

        for (RegisterCustomBlockEntitiesEvent.Entry entry : reg.onRegisterCustomBlockEntities().getEntries()) {
            setItemRenderer(entry.item().get().asItem(), new BlockEntityItemRenderer(entry.type(), entry.state()));
        }

        for (RegisterCustomItemModelsEvent.Entry entry : reg.onRegisterCustomItemModels().getEntries()) {
            setItemRenderer(entry.item().get().asItem(),
                new ModelItemRenderer(entry.layer(), entry.texture(), entry.renderType()));
        }
    }

    private static void setItemRenderer(Item item, BlockEntityWithoutLevelRenderer renderer) {
        ItemRenderPropertiesAccessor accessor = (ItemRenderPropertiesAccessor) item;
        if (accessor.hbs$getRenderProperties() != null) {
            LoggerBase.logWarning(null, "010100", "Item " + item + " already has client render properties, skipping custom item renderer");
            return;
        }
        accessor.hbs$setRenderProperties(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
