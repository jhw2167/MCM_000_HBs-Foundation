package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.PlayerCraftedEvent;
import net.minecraft.world.Container;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks the 3x3 grid a player is crafting in and fires PlayerCraftedEvent on shift click crafting,
 * before vanilla splits the result into the inventory.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private Player player;

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void hbs_foundation$trackGrid(Container container, CallbackInfo ci) {
        if (!this.player.level().isClientSide) PlayerCraftedEvent.CRAFTING_GRIDS.put(this.player, this.craftSlots);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void hbs_foundation$untrackGrid(Player player, CallbackInfo ci) {
        PlayerCraftedEvent.CRAFTING_GRIDS.remove(player, this.craftSlots);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void hbs_foundation$onQuickCraft(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0) return;
        EventRegistrar.onPlayerCrafted(player, ((AbstractContainerMenu) (Object) this).slots.get(0).getItem());
    }
}
