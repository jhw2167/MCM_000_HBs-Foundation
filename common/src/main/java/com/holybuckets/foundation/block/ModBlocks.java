package com.holybuckets.foundation.block;

import com.holybuckets.foundation.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static Block empty;
    public static Block stoneBrickBlockEntity;
    //public static Block[] scopedSharestones = new SharestoneBlock[DyeColor.values().length];

    public static void initialize(BalmBlocks blocks) {
        blocks.register(() -> empty = new EmptyBlock(defaultProperties()), () -> itemBlock(empty), id("empty_block"));
        blocks.register(() -> stoneBrickBlockEntity = new SimpleBlockEntityBlock(SimpleBlockEntityBlock.stoneBrickProperties()), () -> itemBlock(stoneBrickBlockEntity), id("stone_brick_block_entity"));
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


