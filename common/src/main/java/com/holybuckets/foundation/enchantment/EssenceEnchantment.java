package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.core.EssenceType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import javax.annotation.Nullable;

public class EssenceEnchantment extends Enchantment {
    private static final String NBT_ESSENCE_TYPE = "essenceType";
    private final EssenceType type;

    public EssenceEnchantment(EssenceType type) {
        super(Rarity.COMMON, EnchantmentCategory.VANISHABLE, EquipmentSlot.values());
        this.type = type;
    }

    public EssenceEnchantment() {
        this(null);
    }

    @Nullable
    public static EssenceEnchantment of(Item itemType) {
        EssenceType essenceType = EssenceType.of(itemType);
        return (essenceType != null) ? new EssenceEnchantment(essenceType) : null;
    }

    public static ItemStack applyEssenceEnchantment(ItemStack stack, EssenceType essenceType) {
        if (essenceType == null) return stack;

        EssenceEnchantment enchantment = new EssenceEnchantment(essenceType);
        stack.enchant(enchantment, 1);

        // Save essence type to NBT
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_ESSENCE_TYPE, essenceType.getEssenceId());

        return stack;
    }

    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        if (!stack.hasTag()) return null;

        CompoundTag tag = stack.getTag();
        if (!tag.contains(NBT_ESSENCE_TYPE)) return null;

        try {
            return new EssenceType(tag.getString(NBT_ESSENCE_TYPE));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean hasEssenceEnchantment(ItemStack stack) {
        return getEssenceType(stack) != null;
    }


    @Override
    public Component getFullname(int level) {
        MutableComponent name = Component.translatable(this.getDescriptionId());
        if (type != null) {
            name.append(" (").append(type.getEssenceName()).append(")");
        }
        return name.withStyle(ChatFormatting.GRAY);
    }

    @Override
    public boolean isCurse() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    public EssenceType getType() {
        return type;
    }
}