package com.holybuckets.foundation.model;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Built-in resolver for vanilla entities on the server. Handles {@link ServerLevel} only;
 * client-side vanilla resolution lives in the client package
 * ({@code client.core.ClientEntityLikeResolver}) so this common class never references
 * {@code ClientLevel} — which does not exist on a Fabric dedicated server.
 *
 * <p>Registered first in {@link EntityLikeResolver#RESOLVERS} as the cheapest, most
 * common case.</p>
 */
public class VanillaEntityLikeResolver implements EntityLikeResolver {

    public static final VanillaEntityLikeResolver INSTANCE = new VanillaEntityLikeResolver();

    private VanillaEntityLikeResolver() {}

    @Override
    public Optional<EntityLike> resolve(UUID uuid, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        Entity entity = serverLevel.getEntity(uuid);
        if (entity == null || entity.isRemoved()) return Optional.empty();
        return Optional.of(new VanillaEntityLike(entity));
    }
}
