package com.holybuckets.foundation.mixin;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemRenderPropertiesAccessor {

    @Accessor(value = "renderProperties", remap = false)
    Object hbs$getRenderProperties();

    @Accessor(value = "renderProperties", remap = false)
    void hbs$setRenderProperties(Object properties);
}
