package com.holybuckets.foundation.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModelItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final Supplier<ModelLayerLocation> layer;
    private final ResourceLocation texture;
    private final Function<ResourceLocation, RenderType> renderType;
    private ModelPart model;

    public ModelItemRenderer(Supplier<ModelLayerLocation> layer, ResourceLocation texture,
                             Function<ResourceLocation, RenderType> renderType) {
        super(null, null);
        this.layer = layer;
        this.texture = texture;
        this.renderType = renderType;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (model == null) {
            model = Minecraft.getInstance().getEntityModels().bakeLayer(layer.get());
        }
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffers, renderType.apply(texture), false, stack.hasFoil());
        model.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        model = null;
    }
}
