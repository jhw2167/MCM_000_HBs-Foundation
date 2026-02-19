package com.holybuckets.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

public class EssenceCauldronBlock extends LayeredCauldronBlock {

    public EssenceCauldronBlock() {
        super(
            BlockBehaviour.Properties.copy(Blocks.WATER_CAULDRON),
            (p) -> false, Map.of()
        );
    }

    @Override
    public InteractionResult use(BlockState $$0, Level $$1, BlockPos $$2, Player $$3, InteractionHand $$4, BlockHitResult $$5) {
        return InteractionResult.PASS;
    }

}