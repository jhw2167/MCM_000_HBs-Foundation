package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.event.custom.ItemEntityTickEvent;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void onItemEntityTick(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity)(Object)this;
        ItemEntityTickEvent.EVENT.invoke(itemEntity);
    }
}