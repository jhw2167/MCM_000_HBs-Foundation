package com.holybuckets.foundation.item;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.util.DeferredObject;
import net.blay09.mods.balm.world.item.BalmCreativeModeTabRegistrar;
import net.blay09.mods.balm.world.item.BalmItemRegistrar;
import net.blay09.mods.balm.world.item.DeferredItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.holybuckets.foundation.HBUtil;

public class ModItems {
    public static Identifier FOUNDATIONS_TAB = id(Constants.MOD_ID);

    public static DeferredItem enchantedEssenceItem;
    public static DeferredItem waypointStickItem;

    public static DeferredObject<Item> enchantedEssence;
    public static DeferredObject<Item> waypointStick;

    public static void initialize(BalmItemRegistrar items)
    {
        enchantedEssenceItem = items.register("enchanted_essence",e
            props -> new EnchantedEssence(null, props)).asDeferredItem();
        enchantedEssence = DeferredObject.of(enchantedEssenceItem);

        waypointStickItem = items.register("waypoint_stick", WaypointStick::new).asDeferredItem();
        waypointStick = DeferredObject.of(waypointStickItem);
    }

    public static void creativeTab(BalmCreativeModeTabRegistrar tabRegistrar) {
        tabRegistrar.register(Constants.MOD_ID, builder ->
        builder.title(Component.translatable("itemGroup.hbs_foundation.hbs_foundation"))
        .icon(() -> new ItemStack(ModBlocks.stoneBrickBlockEntity.get().asItem()))
            .displayItems((displayParameters, output) -> {
                output.accept(ModBlocks.stoneBrickBlockEntity.get().asItem());
                output.accept(ModItems.enchantedEssence.get());
                output.accept(ModItems.waypointStick.get());
            }
        ));
    }

    private static Identifier id(String name) {
        return HBUtil.LOC(Constants.MOD_ID, name);
    }

}
