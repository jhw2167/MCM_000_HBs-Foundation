package com.holybuckets.foundation.item;


import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.HBUtil;
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

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;
    public static ResourceLocation FOUNDATIONS_TAB = id(Constants.MOD_ID);

    public static SimpleRewardItem enchantedEssence;
    public static WaypointStick waypointStick;

    public static void commonInitialize(BalmItems items)
    {
        items.registerItem(() -> enchantedEssence = new EnchantedEssence(null), id("enchanted_essence"),
        HBUtil.LOC("hbs_traveler_rewards:hbs_traveler_rewards"));
    }

    public static void initialize(BalmItems items) {
        Balm.getRuntime().creativeModeTabs(Constants.MOD_ID, ModItems::creativeTab);
    }

    public static void creativeTab(BalmCreativeModeTabRegistrar tabRegistrar) {
        tabRegistrar.register(Constants.MOD_ID, builder ->
        builder.title(Component.translatable("itemGroup.hbs_foundation.hbs_foundation"))
        .icon(() -> new ItemStack(ModBlocks.stoneBrickBlockEntity.asItem()))
            .displayItems((displayParameters, output) -> {
                output.accept(ModBlocks.stoneBrickBlockEntity.asItem());
                output.accept(ModItems.enchantedEssence);
                output.accept(ModItems.waypointStick);
            }
        ));
    }

    private static ResourceLocation id(String name) {
        return HBUtil.LOC(Constants.MOD_ID, name);
    }

}
