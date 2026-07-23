package com.holybuckets.foundation.client.render;

import com.holybuckets.foundation.block.entity.SimpleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;


public class SimpleBlockEntityRenderer implements BlockEntityRenderer<SimpleBlockEntity, BlockEntityRenderState> {

    public SimpleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // You can use ctx for model loading, texture binding, etc.
    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(BlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        // Your rendering logic goes here
        // If using a baked model, you might not need to do anything here!
    }

}
