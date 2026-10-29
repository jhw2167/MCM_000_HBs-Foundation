package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 2x2 counterpart of CraftingMenuMixin. Vanilla never calls onCraftedBy before a 2x2 shift click moves
 * the result, so the event must fire here or the change would land on the emptied stack.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void hbs_foundation$onQuickCraft(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != InventoryMenu.RESULT_SLOT) return;
        EventRegistrar.onPlayerCrafted(player, this.craftSlots,
            ((AbstractContainerMenu) (Object) this).slots.get(InventoryMenu.RESULT_SLOT).getItem());
    }
}
