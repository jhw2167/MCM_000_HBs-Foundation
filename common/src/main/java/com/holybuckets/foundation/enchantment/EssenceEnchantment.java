package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.core.EssenceType;
import com.holybuckets.foundation.datacomponent.EssenceDataComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;


import javax.annotation.Nullable;

public class EssenceEnchantment {


    public static final Identifier LOC = HBUtil.LOC("hbs_foundation", "essence");
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(
        Registries.ENCHANTMENT, LOC
    );

    public static final Holder<Enchantment> GET(RegistryAccess registryAccess) {
        return registryAccess
            .registryOrThrow(Registries.ENCHANTMENT).getHolder(KEY)
            .orElse(null);
    }


    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        EssenceDataComponent component = stack.get(EssenceDataComponent.TYPE);
        return (component != null) ? component.getEssenceType() : null;
    }

    public static boolean hasEssenceEnchantment(ItemStack stack) {
        return stack.has(EssenceDataComponent.TYPE);
    }

    public static void setEssenceType(ItemStack stack, EssenceType type) {
        stack.set(EssenceDataComponent.TYPE, new EssenceDataComponent(type));
    }

    public static void removeEssenceType(ItemStack stack) {
        stack.remove(EssenceDataComponent.TYPE);
    }

    public static Component getDisplayName(ItemStack stack) {
        EssenceType type = getEssenceType(stack);
        if (type == null) return Component.empty();
        MutableComponent name = Component.translatable(type.getEssenceId());
        return name.withStyle(ChatFormatting.GRAY);
    }

    private EssenceEnchantment() {}
}