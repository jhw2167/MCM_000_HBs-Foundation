package com.holybuckets.foundation.block;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    // Held as Balm DeferredObjects so consumers resolve the real block via .get()
    // AFTER registration, instead of reading a raw static field that is still null
    // while the deferred registration phase is running.
    public static DeferredObject<Block> empty;
    public static DeferredObject<Block> stoneBrickBlockEntity;
    public static DeferredObject<Block> essenceCauldron;
    //public static Block[] scopedSharestones = new SharestoneBlock[DyeColor.values().length];

    public static void initialize(BalmBlocks blocks) {
        ResourceLocation creativeTab = id(Constants.MOD_ID);

        empty = blocks.registerBlock(rl -> new EmptyBlock(defaultProperties()), id("empty_block"));
        blocks.registerBlockItem(rl -> itemBlock(empty.get()), id("empty_block"), creativeTab);

        stoneBrickBlockEntity = blocks.registerBlock(rl -> new SimpleBlockEntityBlock(SimpleBlockEntityBlock.stoneBrickProperties()), id("stone_brick_block_entity"));
        blocks.registerBlockItem(rl -> itemBlock(stoneBrickBlockEntity.get()), id("stone_brick_block_entity"), creativeTab);

        essenceCauldron = blocks.registerBlock(rl -> new EssenceCauldronBlock(), id("essence_cauldron"));
        blocks.registerBlockItem(rl -> itemBlock(essenceCauldron.get()), id("essence_cauldron"), creativeTab);
        /*
        DyeColor[] colors = DyeColor.values();
        for (DyeColor color : colors) {
            blocks.register(() -> scopedSharestones[color.ordinal()] = new SharestoneBlock(defaultProperties(), color), () -> itemBlock(scopedSharestones[color.ordinal()]), id(color.getSerializedName() + "_sharestone"));
        }
        */

    }

    private static BlockItem itemBlock(Block block) {
        return new BlockItem(block, Balm.getItems().itemProperties());
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return Balm.getBlocks().blockProperties().sound(SoundType.STONE).strength(5f, 2000f);
    }

    private static BlockBehaviour.Properties emptyBlockProps() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().copy(Blocks.AIR).forceSolidOn();  //default properties
        return props;
    }

}


