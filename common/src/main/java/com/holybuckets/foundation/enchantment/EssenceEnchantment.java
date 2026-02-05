package com.holybuckets.foundation.enchantment;

import com.holybuckets.foundation.core.EssenceType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import javax.annotation.Nullable;

public class EssenceEnchantment extends Enchantment {
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
}