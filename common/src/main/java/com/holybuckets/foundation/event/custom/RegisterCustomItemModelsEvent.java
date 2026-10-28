package com.holybuckets.foundation.event.custom;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegisterCustomItemModelsEvent {

    public record Entry(Supplier<? extends ItemLike> item,
                        Supplier<ModelLayerLocation> layer,
                        ResourceLocation texture,
                        Function<ResourceLocation, RenderType> renderType) {}

    private final List<Entry> entries = new ArrayList<>();

    public void register(Supplier<? extends ItemLike> item, Supplier<ModelLayerLocation> layer, ResourceLocation texture) {
        register(item, layer, texture, RenderType::entityCutoutNoCull);
    }

    public void register(Supplier<? extends ItemLike> item, Supplier<ModelLayerLocation> layer,
                         ResourceLocation texture, Function<ResourceLocation, RenderType> renderType) {
        entries.add(new Entry(item, layer, texture, renderType));
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
