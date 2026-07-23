package com.holybuckets.foundation.block;

import com.holybuckets.foundation.util.DeferredObject;
import net.blay09.mods.balm.world.level.block.BalmBlockRegistrar;
import net.blay09.mods.balm.world.level.block.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static DeferredBlock emptyBlock;
    public static DeferredBlock stoneBrickBlock;
    public static DeferredBlock essenceCauldronBlock;

    public static DeferredObject<Block> empty;
    public static DeferredObject<Block> stoneBrickBlockEntity;
    public static DeferredObject<Block> essenceCauldron;

    public static void initialize(BalmBlockRegistrar blocks) {
        emptyBlock = blocks.register("empty_block", EmptyBlock::new, ModBlocks::defaultProperties)
            .withDefaultItem()
            .asDeferredBlock();
        empty = DeferredObject.of(emptyBlock);

        stoneBrickBlock = blocks.register("stone_brick_block_entity", SimpleBlockEntityBlock::new, SimpleBlockEntityBlock::stoneBrickProperties)
            .withDefaultItem()
            .asDeferredBlock();
        stoneBrickBlockEntity = DeferredObject.of(stoneBrickBlock);

        essenceCauldronBlock = blocks.register("essence_cauldron", EssenceCauldronBlock::new, EssenceCauldronBlock::cauldronProperties)
            .withDefaultItem()
            .asDeferredBlock();
        essenceCauldron = DeferredObject.of(essenceCauldronBlock);
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(5f, 2000f);
    }

    private static BlockBehaviour.Properties emptyBlockProps() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.AIR).forceSolidOn();
    }

}
