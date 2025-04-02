package com.holybuckets.foundation.block;

import com.holybuckets.foundation.block.entity.SimpleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().copy(Blocks.STONE_BRICKS);  //default properties
        return props;
    }
}

