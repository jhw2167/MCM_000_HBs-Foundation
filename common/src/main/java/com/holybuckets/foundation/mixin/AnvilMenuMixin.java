package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
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

        // Trigger the anvil update event
        EventRegistrar eventRegistrar = EventRegistrar.getInstance();
        if (eventRegistrar != null) {
            eventRegistrar.onAnvilUpdate(menu, left, right);
        }

        // Check if any event handler set a result
        // Note: We need to get the event result somehow. For now, we'll need to modify the event system
        // to return the event or store the result in a way we can access it.
        // This is a simplified approach - you may need to adjust based on your event system design.
        
        // For now, let's assume we have a way to get the last fired event result
        // This would need to be implemented in EventRegistrar to return the event after processing
        // or use a different pattern to get the result back to the mixin
        
        // Placeholder for getting event result - this needs to be implemented properly
        // based on how you want to handle the event result flow
        
        // **CANCEL VANILLA LOGIC** - only if we want to completely override
        // ci.cancel();
    }
}
