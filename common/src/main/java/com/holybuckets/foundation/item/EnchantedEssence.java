package com.holybuckets.foundation.item;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.core.EssenceType;
import com.holybuckets.foundation.datacomponent.EssenceDataComponent;
import com.holybuckets.foundation.enchantment.EssenceEnchantment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class EnchantedEssence extends SimpleRewardItem {
    
    @Nullable
    private final EssenceEnchantment essenceEnchantment;

    public static final String ESSENCE_DATA_TAG = "EssenceData";

    public EnchantedEssence(@Nullable EssenceEnchantment essenceEnchantment, Item.Properties properties) {
        super("enchanted_essence", properties);
        this.essenceEnchantment = essenceEnchantment;
    }
    
    @Nullable
    public EssenceEnchantment getEssenceEnchantment() {
        return essenceEnchantment;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipConsumer, TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, context, display, tooltipConsumer, isAdvanced);
        EssenceType type = EssenceDataComponent.getEssenceType(stack);
        if(type != null)
        {
            String id = type.getEssenceId();
            Set<Holder<Biome>> biomes = EssenceType.getBiomes(id);
            String list = "Target Biomes: ";
            if (!biomes.isEmpty()) {
                 list += HBUtil.LevelUtil.getBiomeSimpleNames(biomes);
            } else {
                list = "No Biomes Associated with this Essence";
            }
            tooltipConsumer.accept(Component.literal(list).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipConsumer.accept(Component.translatable(modDescKey));
        }
    }
    
    @Override
    public Component getName(ItemStack stack)
    {
        EssenceType type = EssenceDataComponent.getEssenceType(stack);
        if(type != null) {
            String fullName = type.getEssenceName() +" Essence";
            return Component.literal(fullName);
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        return stack;
    }
}
