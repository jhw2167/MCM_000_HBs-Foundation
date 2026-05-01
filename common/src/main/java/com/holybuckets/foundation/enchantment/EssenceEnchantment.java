package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.core.EssenceType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import com.holybuckets.foundation.core.EssenceType;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;


import javax.annotation.Nullable;

public class EssenceEnchantment {


    public static final ResourceLocation LOC = ResourceLocation.fromNamespaceAndPath("hbs_foundation", "essence");
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(
        Registries.ENCHANTMENT, LOC
    );


    static final class Definition {

        // Slot tag used in the JSON definition
        static final String SLOT = "any";

        // Not discoverable via table or trades — applied only programmatically
        static final int WEIGHT          = 0;
        static final int MAX_LEVEL       = 1;
        static final int MIN_COST_BASE   = 1;
        static final int MAX_COST_BASE   = 41;
        static final int ANVIL_COST      = 0;

        private Definition() {}
    }


    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        EssenceComponent component = stack.get(EssenceComponent.TYPE);
        return (component != null) ? component.getEssenceType() : null;
    }

    public static boolean hasEssenceEnchantment(ItemStack stack) {
        return stack.has(EssenceComponent.TYPE);
    }

    public static void setEssenceType(ItemStack stack, EssenceType type) {
        stack.set(EssenceComponent.TYPE, new EssenceComponent(type));
    }

    public static void removeEssenceType(ItemStack stack) {
        stack.remove(EssenceComponent.TYPE);
    }

    public static Component getDisplayName(ItemStack stack) {
        EssenceType type = getEssenceType(stack);
        if (type == null) return Component.empty();
        MutableComponent name = Component.translatable(type.getEssenceId());
        return name.withStyle(ChatFormatting.GRAY);
    }

    private EssenceEnchantment() {}
}