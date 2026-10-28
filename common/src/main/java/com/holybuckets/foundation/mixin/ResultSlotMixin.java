package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Click, drop and hotbar swap claims all pass the claimed stack itself through onTake before the grid is consumed.
 * Shift click reaches here with the already moved, emptied stack, so it is skipped by the empty check.
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void hbs_foundation$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        EventRegistrar.onPlayerCrafted(player, stack);
    }
}
