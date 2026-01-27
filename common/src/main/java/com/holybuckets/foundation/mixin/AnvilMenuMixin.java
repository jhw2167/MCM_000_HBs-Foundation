package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
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
public abstract class AnvilMenuMixin {  // Don't extend ItemCombinerMenu

    @Shadow
    @Final
    private DataSlot cost;

    // Shadow the result slots from ItemCombinerMenu (parent class)
    @Shadow
    protected net.minecraft.world.Container resultSlots;

    @Inject(
        method = "createResult",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu)(Object)this;

        // Get input slots
        ItemStack left = menu.slots.get(0).getItem();   // Left slot
        ItemStack right = menu.slots.get(1).getItem();  // Right slot

        // Validate
        if (left.isEmpty()) {
            return;
        }

        // Trigger the anvil update event
        EventRegistrar eventRegistrar = EventRegistrar.getInstance();
        AnvilUpdateEvent event = new AnvilUpdateEvent(menu, left, right);
        if (eventRegistrar != null) {
            eventRegistrar.onAnvilUpdate(event);
        }

        // Check if event produced a result
        ItemStack resultItem = event.getResultItem();
        if (resultItem != null && !resultItem.isEmpty())
        {

            this.resultSlots.setItem(0, resultItem);
            int resultCost = event.getResultCost();
            this.cost.set(resultCost);

            ci.cancel();
            menu.broadcastChanges();
        }
        // If event didn't set a result, let vanilla logic continue
    }
}