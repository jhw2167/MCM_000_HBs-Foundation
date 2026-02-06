package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;


    @Inject(
        method = "createResult",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu)(Object)this;

        // Get input items via menu slots
        ItemStack left = menu.slots.get(0).getItem();
        ItemStack right = menu.slots.get(1).getItem();

        if (left.isEmpty()) {
            return;
        }

        // Fire event
        EventRegistrar eventRegistrar = EventRegistrar.getInstance();
        if (eventRegistrar == null) {
            return;
        }

        AnvilUpdateEvent event = new AnvilUpdateEvent(menu, left, right);
        AnvilUpdateEvent.ANVIL_EVENTS.put(menu, event);
        eventRegistrar.onAnvilUpdate(event);

        // Check if event produced a result
        ItemStack resultItem = event.getResultItem();
        if (resultItem != null && !resultItem.isEmpty()) {
            // Set output slot directly - NO resultSlots shadow needed
            menu.slots.get(2).set(resultItem);
            this.cost.set(event.getResultCost());
            this.repairItemCountCost = event.getRepairItemCost();
            ci.cancel();

            // Sync
            menu.broadcastChanges();
        }
    }
    //END ONCREATE RESULT

    @Inject(
        method = "onTake",
        at = @At("HEAD"),
        cancellable = true
    )
    protected void onOnTake(Player $$0, ItemStack $$1, CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu)(Object)this;
        AnvilUpdateEvent event = AnvilUpdateEvent.ANVIL_EVENTS.remove(menu);
        if(event==null || event.getMainItemCost()==0 || event.getLeftItem()==null) return;
        event.getLeftItem().shrink(event.getMainItemCost());
    }
}
