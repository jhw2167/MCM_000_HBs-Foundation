package com.holybuckets.foundation.item;

import com.holybuckets.foundation.util.DeferredObject;
import net.blay09.mods.balm.world.item.BalmCreativeModeTabRegistrar;
import net.blay09.mods.balm.world.item.DeferredItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

public class CreativeTabRegistry {

    public static void registerTab(BalmCreativeModeTabRegistrar tabs, String modId, DeferredItem icon, List<DeferredObject<Item>> items) {
        tabs.register(modId, builder ->
            builder.title(Component.translatable("itemGroup." + modId + "." + modId))
                .icon(icon::createStack)
                .displayItems((displayParameters, output) -> items.forEach( it -> output.accept(it.get())))
        );
    }

}