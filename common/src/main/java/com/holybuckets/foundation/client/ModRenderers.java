package com.holybuckets.foundation.client;

import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.block.entity.ModBlockEntities;
import com.holybuckets.foundation.client.render.SimpleBlockEntityRenderer;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.minecraft.client.renderer.RenderType;

public class ModRenderers {

    //public static ModelLayerLocation someModel;

    public static void clientInitialize(BalmRenderers renderers) {
        //waystoneModel = renderers.registerModel(new ResourceLocation(Waystones.MOD_ID, "waystone"), () -> WaystoneModel.createLayer(CubeDeformation.NONE));

        renderers.registerBlockEntityRenderer(ModBlockEntities.simpleBlockEntity::get, SimpleBlockEntityRenderer::new);

        //renderers.registerBlockColorHandler((state, view, pos, tintIndex) -> Objects.requireNonNull(((SharestoneBlock) state.getBlock()).getColor()).getTextColor(), () -> ModBlocks.scopedSharestones);
        //renderers.registerItemColorHandler((stack, tintIndex) -> Objects.requireNonNull(((SharestoneBlock) Block.byItem((stack.getItem()))).getColor()).getTextColor(), () -> ModBlocks.scopedSharestones);

        renderers.setBlockRenderType(() -> ModBlocks.stoneBrickBlockEntity, RenderType.cutout());
    }

}
