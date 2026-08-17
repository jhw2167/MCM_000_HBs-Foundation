package com.holybuckets.foundation.effect;

import com.holybuckets.foundation.HBUtil;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import java.util.function.Supplier;

/**
 * Baseline registration support for mob effects and potions. Balm has no convenience registrar for
 * either registry.
 */
public class EffectRegistry {

    public static Holder<MobEffect> registerEffect(BalmRegistrar registrar, String modId, String name, Supplier<MobEffect> effect) {
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, id(modId, name));
        return registrar.register(key, loc -> effect.get());
    }

    public static Holder<Potion> registerPotion(BalmRegistrar registrar, String modId, String name, Supplier<Potion> potion) {
        ResourceKey<Potion> key = ResourceKey.create(Registries.POTION, id(modId, name));
        return registrar.register(key, loc -> potion.get());
    }

    public static Holder<Potion> registerPotion(BalmRegistrar registrar, String modId, String name, Holder<MobEffect> effect, int durationTicks, int amplifier) {
        return registerPotion(registrar, modId, name,
            () -> new Potion(name, new MobEffectInstance(effect, durationTicks, amplifier)));
    }

    private static ResourceLocation id(String modId, String name) {
        return HBUtil.LOC(modId, name);
    }

}
