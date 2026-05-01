package com.holybuckets.foundation.datacomponent;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.core.EssenceType;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;

public class EssenceTypeData {

    public static final ResourceLocation LOC = ResourceLocation
        .fromNamespaceAndPath(Constants.MOD_ID, "essence_type");

    public static final Codec<EssenceTypeData> CODEC = Codec.STRING
        .xmap(EssenceTypeData::new, c -> c.essenceType.getEssenceId());

    public static final DataComponentType<EssenceTypeData> TYPE = DataComponentType
        .<EssenceTypeData>builder()
        .persistent(CODEC)
        .build();

    private final EssenceType essenceType;

    public EssenceTypeData(String essenceId) {
        this.essenceType = new EssenceType(essenceId);
    }

    public EssenceTypeData(EssenceType essenceType) {
        this.essenceType = essenceType;
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }
}