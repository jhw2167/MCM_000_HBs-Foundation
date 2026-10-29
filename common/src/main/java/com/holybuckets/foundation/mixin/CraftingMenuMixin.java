package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires PlayerCraftedEvent on 3x3 shift click crafting, before vanilla splits the result into the inventory.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void hbs_foundation$onQuickCraft(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0) return;
        EventRegistrar.onPlayerCrafted(player, this.craftSlots, ((AbstractContainerMenu) (Object) this).slots.get(0).getItem());
    }
}
