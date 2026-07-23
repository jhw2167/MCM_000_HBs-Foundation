package com.holybuckets.foundation.event.balm;

import net.minecraft.world.entity.LivingEntity;

public class LivingFallEvent extends BalmEvent {
    private final LivingEntity entity;
    private final float fallDamage;
    private Float fallDamageOverride;

    public LivingFallEvent(LivingEntity entity, float fallDamage) {
        this.entity = entity;
        this.fallDamage = fallDamage;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public float getFallDamage() {
        return fallDamage;
    }

    public Float getFallDamageOverride() {
        return fallDamageOverride;
    }

    public void setFallDamageOverride(Float fallDamageOverride) {
        this.fallDamageOverride = fallDamageOverride;
    }
}
