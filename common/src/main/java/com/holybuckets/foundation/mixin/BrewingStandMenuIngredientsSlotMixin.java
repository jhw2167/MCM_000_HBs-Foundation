package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets players place event driven ingredients into the brewing stand, by hand and by shift click.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot")
public abstract class BrewingStandMenuIngredientsSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void hbs_foundation$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        EventRegistrar registrar = EventRegistrar.getInstance();
        if (registrar != null && registrar.isBrewingIngredient(stack)) cir.setReturnValue(true);
    }
}
