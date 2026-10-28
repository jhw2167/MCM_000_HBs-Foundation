package com.holybuckets.foundation.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class BlockEntityItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final Supplier<? extends BlockEntityType<?>> type;
    private final Supplier<BlockState> state;
    private BlockEntity blockEntity;

    public BlockEntityItemRenderer(Supplier<? extends BlockEntityType<?>> type, Supplier<BlockState> state) {
        super(null, null);
        this.type = type;
        this.state = state;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (blockEntity == null) {
            blockEntity = type.get().create(BlockPos.ZERO, state.get());
            if (blockEntity == null) return;
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(blockEntity, pose, buffers, light, overlay);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
    }
}
