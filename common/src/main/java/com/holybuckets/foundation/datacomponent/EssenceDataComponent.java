package com.holybuckets.foundation.datacomponent;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.core.EssenceType;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EssenceDataComponent {

    public static final ResourceLocation LOC = ResourceLocation
        .fromNamespaceAndPath(Constants.MOD_ID, "essence_type");

    public static final Codec<EssenceDataComponent> CODEC = Codec.STRING
        .xmap(EssenceDataComponent::new, c -> c.essenceType.getEssenceId());

    public static final DataComponentType<EssenceDataComponent> TYPE = DataComponentType
        .<EssenceDataComponent>builder()
        .persistent(CODEC)
        .build();

    private final EssenceType essenceType;

    public EssenceDataComponent(String essenceId) {
        this.essenceType = new EssenceType(essenceId);
    }

    public EssenceDataComponent(EssenceType essenceType) {
        this.essenceType = essenceType;
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }


    public static void createFromItem(ItemStack essence, Item item) {
        create(essence, EssenceType.of(item));
    }

    public static void create(ItemStack essence, EssenceType t) {
        essence.set(TYPE, new EssenceDataComponent(t));
    }

    public static EssenceType getEssenceType(ItemStack stack) {
        EssenceDataComponent component = stack.get(TYPE);
        return (component != null) ? component.getEssenceType() : null;
    }

}