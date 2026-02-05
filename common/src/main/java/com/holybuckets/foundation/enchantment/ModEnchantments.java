package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    public static DeferredObject<Enchantment> ESSENCE;

    public static void register() {
        ESSENCE = Balm.getRegistries().register( BuiltInRegistries.ENCHANTMENT,
            id -> new EssenceEnchantment(),
            id("essence")
        );
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}