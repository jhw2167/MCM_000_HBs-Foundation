package com.holybuckets.foundation.block.entity;

import net.blay09.mods.balm.common.BalmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that indicates the bottom of a portal
 */
public class SimpleBlockEntity extends BalmBlockEntity {

    public SimpleBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.simpleBlockEntity.get(), blockPos, blockState);
    }


}
