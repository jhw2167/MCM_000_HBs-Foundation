package com.holybuckets.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

public class EssenceCauldronBlock extends LayeredCauldronBlock {

    public EssenceCauldronBlock(BlockBehaviour.Properties properties) {
        super(Biome.Precipitation.NONE, new CauldronInteraction.Dispatcher() , properties);
    }

    public static BlockBehaviour.Properties cauldronProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WATER_CAULDRON);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack $$0, BlockState $$1, Level $$2, BlockPos $$3, Player $$4, InteractionHand $$5, BlockHitResult $$6) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

}
