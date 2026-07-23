package com.holybuckets.foundation.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A read-only, loader- and backend-agnostic view of "something a waypoint can track".
 *
 * <p>Foundation resolves a waypoint's {@code linkedEntityUuid} to one of these via the
 * {@link EntityLikeResolver} list, instead of assuming the UUID always belongs to a
 * vanilla {@link Entity}. The built-in implementation ({@link VanillaEntityLike}) wraps a
 * vanilla entity; other mods (e.g. HBs Aero Waypoints wrapping a Sable sub-level) supply
 * their own implementations without Foundation ever knowing about them.</p>
 */
public interface EntityLike {

    /** The UUID identifying this target (matches the waypoint's {@code linkedEntityUuid}). */
    UUID getUUID();

    /** Current world-space position. */
    Vec3 position();

    /** Current yaw, in degrees. */
    float getYRot();

    /** Current pitch, in degrees. */
    float getXRot();

    /** The dimension the target is currently in. */
    Identifier dimension();

    /** False once the target is removed / disassembled / no longer trackable. */
    boolean isValid();

    /** Block position derived from {@link #position()}; overridable when a target exposes it directly. */
    default BlockPos blockPosition() {
        Vec3 p = position();
        return BlockPos.containing(p.x, p.y, p.z);
    }

    /** Convenience wrapper for a vanilla entity (null-safe). */
    static EntityLike of(Entity entity) {
        return entity == null ? null : new VanillaEntityLike(entity);
    }
}
