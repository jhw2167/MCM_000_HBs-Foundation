package com.holybuckets.foundation.model;

import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Resolves UUId to some entity like - minecraft entity or sable sublevel
 */
public interface EntityLikeResolver {


    Optional<EntityLike> resolve(UUID uuid, Level level);

    List<EntityLikeResolver> RESOLVERS =
        new CopyOnWriteArrayList<>(List.of(VanillaEntityLikeResolver.INSTANCE));

    //other mods with proper dependencies register resolver
    static void register(EntityLikeResolver resolver) {
        if (resolver != null && !RESOLVERS.contains(resolver)) {
            RESOLVERS.add(resolver);
        }
    }

    //first match wins
    static Optional<EntityLike> resolveEntity(UUID uuid, Level level) {
        if (uuid == null || level == null) return Optional.empty();
        for (EntityLikeResolver resolver : RESOLVERS) {
            Optional<EntityLike> result = resolver.resolve(uuid, level);
            if (result != null && result.isPresent()) return result;
        }
        return Optional.empty();
    }
}
