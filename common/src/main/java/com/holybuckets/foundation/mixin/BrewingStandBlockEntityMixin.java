package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.BrewingUpdateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires BrewingUpdateEvent CHECK from isBrewable and BREW from doBrew. Both methods keep their vanilla
 * signatures on Forge, which diverts their bodies to BrewingRecipeRegistry, so one mixin covers both loaders.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    /** isBrewable only receives the slot list, so serverTick shares the ticking stand with it */
    @Unique
    private static final ThreadLocal<BrewingStandBlockEntity> hbs_foundation$TICKING = new ThreadLocal<>();

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void hbs_foundation$onTickStart(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        hbs_foundation$TICKING.set(be);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void hbs_foundation$onTickEnd(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        hbs_foundation$TICKING.remove();
    }

    @Inject(method = "isBrewable", at = @At("HEAD"), cancellable = true)
    private static void hbs_foundation$isBrewable(NonNullList<ItemStack> items, CallbackInfoReturnable<Boolean> cir)
    {
        EventRegistrar registrar = EventRegistrar.getInstance();
        if (registrar == null || !registrar.hasBrewingHandlers()) return;

        BrewingStandBlockEntity be = hbs_foundation$TICKING.get();
        Level level = (be == null) ? null : be.getLevel();
        BlockPos pos = (be == null) ? null : be.getBlockPos();
        for (BrewingUpdateEvent event : registrar.fireBrewingUpdates(BrewingUpdateEvent.Phase.CHECK, level, pos, be, items)) {
            if (event != null) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void hbs_foundation$doBrew(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo ci)
    {
        EventRegistrar registrar = EventRegistrar.getInstance();
        if (registrar == null || !registrar.hasBrewingHandlers()) return;

        BrewingUpdateEvent[] events = registrar.fireBrewingUpdates(BrewingUpdateEvent.Phase.BREW, level, pos, hbs_foundation$TICKING.get(), items);
        int cost = 0;
        for (int i = 0; i < events.length; i++) {
            if (events[i] == null) continue;
            items.set(i, events[i].getResultItem().copy());
            cost = Math.max(cost, events[i].getIngredientCost());
        }
        if (cost == 0) return;

        //Mirrors vanilla ingredient consumption, including crafting remainders
        ItemStack ingredient = items.get(BrewingUpdateEvent.INGREDIENT_SLOT);
        ItemStack remainder = ingredient.getItem().hasCraftingRemainingItem()
            ? new ItemStack(ingredient.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;
        ingredient.shrink(cost);
        if (!remainder.isEmpty()) {
            if (ingredient.isEmpty()) ingredient = remainder;
            else Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder);
        }
        items.set(BrewingUpdateEvent.INGREDIENT_SLOT, ingredient);
        level.levelEvent(1035, pos, 0);
        ci.cancel();
    }

    /** Hopper and automation insertion into the ingredient slot */
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void hbs_foundation$canPlaceItem(int index, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (index != BrewingUpdateEvent.INGREDIENT_SLOT) return;
        EventRegistrar registrar = EventRegistrar.getInstance();
        if (registrar != null && registrar.isBrewingIngredient(stack)) cir.setReturnValue(true);
    }
}
