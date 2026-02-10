package com.holybuckets.foundation.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class EnchantedEssence extends SimpleRewardItem {
    
    @Nullable
    private final EssenceEnchantment essenceEnchantment;
    
    public EnchantedEssence(Properties properties) {
        this(properties, null);
    }
    
    public EnchantedEssence(Properties properties, @Nullable EssenceEnchantment essenceEnchantment) {
        super(properties);
        this.essenceEnchantment = essenceEnchantment;
    }
    
    @Nullable
    public EssenceEnchantment getEssenceEnchantment() {
        return essenceEnchantment;
    }
    
    public boolean hasEssenceEnchantment() {
        return essenceEnchantment != null;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        
        if (hasEssenceEnchantment()) {
            // Add the full name with description
            String fullName = essenceEnchantment.getFullName();
            String description = essenceEnchantment.getDescription();
            
            if (fullName != null && !fullName.isEmpty()) {
                tooltipComponents.add(Component.literal(fullName).withStyle(ChatFormatting.GOLD));
            }
            
            if (description != null && !description.isEmpty()) {
                tooltipComponents.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
            }
        }
    }
    
    @Override
    public Component getName(ItemStack stack) {
        if (hasEssenceEnchantment()) {
            String fullName = essenceEnchantment.getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                return Component.literal(fullName);
            }
        }
        
        return super.getName(stack);
    }
}
