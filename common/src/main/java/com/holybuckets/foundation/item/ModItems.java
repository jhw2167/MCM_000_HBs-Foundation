package com.holybuckets.foundation.item;


import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.block.ModBlocks;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.item.BalmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;
    public static ResourceLocation FOUNDATIONS_TAB = id(Constants.MOD_ID);

    public static Item emptyBlockItem;
    public static DeferredObject<Item> enchantedEssence;
    public static DeferredObject<Item> waypointStick;

    public static void commonInitialize(BalmItems items)
    {
        // Register the enchanted essence item (added to the creative tab manually below).
        enchantedEssence = items.registerItem(rl -> new SimpleRewardItem(Constants.MOD_ID, "enchanted_essence"), id("enchanted_essence"),
         new ResourceLocation("hbs_traveler_rewards:hbs_traveler_rewards"));
    }

    public static void initialize(BalmItems items) {
        // Register the waypoint stick (added to the creative tab manually below).
        waypointStick = items.registerItem(rl -> new WaypointStick(), id("waypoint_stick"), null);

        creativeModeTab = items.registerCreativeModeTab(
            () -> new ItemStack( ModBlocks.stoneBrickBlockEntity.get().asItem() ), FOUNDATIONS_TAB);
        // Blocks add themselves to FOUNDATIONS_TAB via their registerBlockItem call
        // (see ModBlocks). Here we only add the non-block items.
        items.addToCreativeModeTab(FOUNDATIONS_TAB, () -> new ItemLike[]{
            ModItems.enchantedEssence.get(),
            ModItems.waypointStick.get()
        });


    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
