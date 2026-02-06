package com.holybuckets.foundation.item;

import net.blay09.mods.balm.api.Balm;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base class for simple reward items that have no special behavior beyond tooltips.
 * Used for crafting materials, ingredients, and basic reward items.
 *
 * Examples: Enchanted Essence, Iron Bloom, Diamond Shard, Savior Orb
 */
public class SimpleRewardItem extends Item {

    protected final String itemId;
    protected final String modDescKey;

    public SimpleRewardItem(String itemId) {
        this(itemId, "item.hbs_foundation." + itemId + ".desc");
    }

    public SimpleRewardItem(String itemId, String modDescKey) {
        super(Balm.getItems().itemProperties());
        this.itemId = itemId;
        this.modDescKey = modDescKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable(modDescKey));
    }

    public String getItemId() {
        return itemId;
    }
}