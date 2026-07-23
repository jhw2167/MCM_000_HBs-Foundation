package com.holybuckets.foundation.datacomponent;


import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.HBUtil;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public class ModDataComponents {

    public static Holder<DataComponentType<?>> ESSENCE_TYPE_COMPONENT;

    public static void register(BalmRegistrar registrar) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
            Registries.DATA_COMPONENT_TYPE, EssenceDataComponent.LOC
        );

        ESSENCE_TYPE_COMPONENT = registrar.register(key, loc -> EssenceDataComponent.TYPE);
    }

    private static Identifier id(String name) {
        return HBUtil.LOC(Constants.MOD_ID, name);
    }
}