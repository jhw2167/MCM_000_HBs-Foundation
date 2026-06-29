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
    public static SimpleRewardItem enchantedEssence;
    public static WaypointStick waypointStick;

    public static void commonInitialize(BalmItems items)
    {
        items.registerItem(() -> enchantedEssence = new EnchantedEssence(null), id("enchanted_essence"),
        new ResourceLocation("hbs_traveler_rewards:hbs_traveler_rewards"));
    }

    public static void initialize(BalmItems items) {
        //items.registerItem(() -> emptyBlockItem = new EmptyBlockItem(items.itemProperties()), id("empty_block"));

        // Creative tab for the foundation mod — use the waypoint stick as the icon.
        creativeModeTab = items.registerCreativeModeTab(FOUNDATIONS_TAB, () -> new ItemStack(waypointStick));

        items.registerItem(() -> waypointStick = new WaypointStick(), id("waypoint_stick"));

        items.addToCreativeModeTab(FOUNDATIONS_TAB, () -> new ItemLike[]{
            ModItems.waypointStick
        });
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
