package com.holybuckets.foundation.item;


import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.block.ModBlocks;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.item.BalmItems;
import net.blay09.mods.balm.world.item.BalmCreativeModeTabRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import  com.holybuckets.foundation.HBUtil;

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;
    public static ResourceLocation FOUNDATIONS_TAB = id(Constants.MOD_ID);

    public static DeferredObject<Item> enchantedEssence;
    public static DeferredObject<Item> waypointStick;

    public static void commonInitialize(BalmItems items)
    {
        // Register the enchanted essence item (added to the creative tab manually below).
        enchantedEssence = items.registerItem(rl -> new SimpleRewardItem(Constants.MOD_ID, "enchanted_essence"), id("enchanted_essence"),
         id("hbs_traveler_rewards"));
    }

    public static void initialize(BalmItems items) {
        // Register the waypoint stick (added to the creative tab manually below).
        waypointStick = items.registerItem(rl -> new WaypointStick(), id("waypoint_stick"), null);

        Balm.getRuntime().creativeModeTabs(Constants.MOD_ID, ModItems::creativeTab);
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

    private static ResourceLocation id(String name) {
        return HBUtil.LOC(Constants.MOD_ID, name);
    }

}
