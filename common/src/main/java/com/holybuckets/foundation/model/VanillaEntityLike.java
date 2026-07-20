package com.holybuckets.foundation.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;


public class VanillaEntityLike implements EntityLike {

    private final Entity entity;

    public VanillaEntityLike(Entity entity) {
        this.entity = entity;
    }

    public Entity entity() {
        return entity;
    }

    @Override
    public UUID getUUID() {
        return entity.getUUID();
    }

    @Override
    public Vec3 position() {
        return entity.position();
    }

    @Override
    public float getYRot() {
        return entity.getYRot();
    }

    @Override
    public float getXRot() {
        return entity.getXRot();
    }

    @Override
    public ResourceLocation dimension() {
        return entity.level().dimension().location();
    }

    @Override
    public boolean isValid() {
        return !entity.isRemoved();
    }

    @Override
    public BlockPos blockPosition() {
        return entity.blockPosition();
    }
}
