package com.holybuckets.foundation.item;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.core.EssenceType;
import com.holybuckets.foundation.enchantment.EssenceEnchantment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class EnchantedEssence extends SimpleRewardItem {
    
    @Nullable
    private final EssenceEnchantment essenceEnchantment;

    public static final String ESSENCE_DATA_TAG = "EssenceData";

    public EnchantedEssence(@Nullable EssenceEnchantment essenceEnchantment) {
        super("enchanted_essence");
        this.essenceEnchantment = essenceEnchantment;
    }
    
    @Nullable
    public EssenceEnchantment getEssenceEnchantment() {
        return essenceEnchantment;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        
        if (stack.getOrCreateTag().contains(ESSENCE_DATA_TAG)) {
            String id = stack.getTag().getString(ESSENCE_DATA_TAG);
            Set<Holder<Biome>> biomes = EssenceType.getBiomes(id);

            if (!biomes.isEmpty()) {
                String list = "Target Biomes: " + HBUtil.LevelUtil.getBiomeSimpleNames(biomes);
                tooltipComponents.add(Component.literal(list).withStyle(ChatFormatting.GRAY));
            }
        }
    }
    
    @Override
    public Component getName(ItemStack stack) {
        if (stack.getOrCreateTag().contains(ESSENCE_DATA_TAG)) {
            String id = stack.getTag().getString(ESSENCE_DATA_TAG);
            String fullName = EssenceType.getEssenceName(id) +" Essence";

            return Component.literal(fullName);
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.getOrCreateTag();
        return stack;
    }
}
