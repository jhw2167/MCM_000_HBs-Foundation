package com.holybuckets.foundation.effect;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModEffects {

    public static DeferredObject<MobEffect> BUILDERS_FLIGHT;

    public static void register() {
        BUILDERS_FLIGHT = Balm.getRegistries().register(
            BuiltInRegistries.MOB_EFFECT,
            id -> new BuildersFlightEffect(),
            id("builders_flight")
        );
    }

    public static class BuildersFlightEffect extends MobEffect {
        public static final int COLOR = 0xFFFFFF;
        protected BuildersFlightEffect() {
            super(MobEffectCategory.BENEFICIAL, COLOR);
        }
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}
