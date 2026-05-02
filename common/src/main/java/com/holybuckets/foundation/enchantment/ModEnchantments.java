package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.HBUtil;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    public static DeferredObject<Enchantment> ESSENCE;

    public static void register() {
        //moved to datapack registry in 1.21
    }

    private static ResourceLocation id(String name) {
        return HBUtil.LOC(Constants.MOD_ID, name);
    }
}