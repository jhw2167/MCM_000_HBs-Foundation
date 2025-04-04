package com.holybuckets.foundation.block;

import com.holybuckets.foundation.block.entity.SimpleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimpleBlockEntityBlock extends BaseEntityBlock {

    protected SimpleBlockEntityBlock(BlockBehaviour.Properties $$0) {
        super($$0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SimpleBlockEntity(blockPos, blockState);
    }

    static BlockBehaviour.Properties stoneBrickProperties() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS);  //default properties
        return props;
    }

    @Override
    public RenderShape getRenderShape(BlockState $$0) {
        return RenderShape.MODEL;
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state)
    {
        if (world.isClientSide()) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be != null) {
            be.setRemoved();
        }

        super.destroy(world, pos, state);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(world, player, pos, state);
        if (world.isClientSide) return;

        // Don't drop anything in creative mode
        if (!player.isCreative()) {
            ItemStack held = player.getMainHandItem();
            if (held.isCorrectToolForDrops(state)) {
                popResource(world, pos, new ItemStack(Items.STONE_BRICKS));
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

}

