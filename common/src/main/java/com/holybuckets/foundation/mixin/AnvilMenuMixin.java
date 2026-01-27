package com.holybuckets.foundation.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    @Inject(
        method = "createResult",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreateResult(CallbackInfo ci)
    {
        AnvilMenu menu = (AnvilMenu)(Object)this;

        // Get input slots
        ItemStack left = this.inputSlots.getItem(0);   // Left slot
        ItemStack right = this.inputSlots.getItem(1);  // Right slot

        // Validate
        if (left.isEmpty()) {
            return;
        }

        // Create output
        ItemStack output = left.copy();

        // Apply enchantment
        switch (enchantItem.getEnchantmentType()) {
            case SHARPNESS:
                int level = enchantItem.getTier() == 1 ? 1 : 3;
                output.enchant(Enchantments.SHARPNESS, level);
                break;
            // ... other cases
        }

        // **SET THE OUTPUT SLOT** - This is the key part
        this.resultSlots.setItem(0, output);

        // **SET THE COST**
        this.cost.set(enchantItem.getTier() == 1 ? 5 : 10);

        // **CANCEL VANILLA LOGIC**
        ci.cancel();
    }
}