package com.holybuckets.foundation.item;

import com.holybuckets.foundation.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Base class for simple reward items that have no special behavior beyond tooltips.
 * Used for crafting materials, ingredients, and basic reward items.
 *
 * Examples: Enchanted Essence, Iron Bloom, Diamond Shard, Savior Orb
 */
public class SimpleRewardItem extends Item {

    protected final String itemId;
    protected final String modDescKey;

    public SimpleRewardItem(String itemId, Item.Properties properties) {
        this(itemId, Constants.MOD_ID, properties);
    }

    public SimpleRewardItem(String itemId, String modId, Item.Properties properties) {
        super(properties);
        this.itemId = itemId;
        this.modDescKey = "item." + modId + "." + itemId + ".desc";
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipConsumer, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, display, tooltipConsumer, isAdvanced);
        tooltipConsumer.accept(Component.translatable(modDescKey));
    }

    public String getItemId() {
        return itemId;
    }
}
