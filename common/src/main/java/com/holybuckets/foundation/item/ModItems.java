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

    public static void commonInitialize(BalmItems items)
    {
        creativeModeTab = items.registerCreativeModeTab( () -> new ItemStack(ModBlocks.empty), FOUNDATIONS_TAB);
    }

    public static void initialize(BalmItems items) {
        //items.registerItem(() -> emptyBlockItem = new EmptyBlockItem(items.itemProperties()), id("empty_block"));

        items.addToCreativeModeTab(FOUNDATIONS_TAB, () -> new ItemLike[]{
            ModBlocks.empty,
            ModBlocks.stoneBrickBlockEntity,
        });
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
