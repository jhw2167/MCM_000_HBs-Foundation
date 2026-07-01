package com.holybuckets.foundation.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin that exposes {@link ClientLevel#getEntities()} (protected in vanilla)
 * so the foundation can resolve entities by UUID on the client without reflection.
 *
 * Usage: {@code ((ClientLevelAccessor)(Object) clientLevel).hbs$getEntities()}
 */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {

    @Invoker("getEntities")
    LevelEntityGetter<Entity> hbs$getEntities();
}
