package com.holybuckets.foundation.client.core;

import com.holybuckets.foundation.mixin.ClientLevelAccessor;
import com.holybuckets.foundation.model.EntityLike;
import com.holybuckets.foundation.model.EntityLikeResolver;
import com.holybuckets.foundation.model.VanillaEntityLike;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;

import java.util.Optional;
import java.util.UUID;

//converts Entity into EntityLike for client side
public class ClientEntityLikeResolver implements EntityLikeResolver {

    public static final ClientEntityLikeResolver INSTANCE = new ClientEntityLikeResolver();

    private ClientEntityLikeResolver() {}

    @Override
    public Optional<EntityLike> resolve(UUID uuid, Level level) {
        if (!(level instanceof ClientLevel clientLevel)) return Optional.empty();
        LevelEntityGetter<Entity> getter = ((ClientLevelAccessor) (Object) clientLevel).getEntityGetter();
        Entity entity = getter.get(uuid);
        if (entity == null || entity.isRemoved()) return Optional.empty();
        return Optional.of(new VanillaEntityLike(entity));
    }
}
