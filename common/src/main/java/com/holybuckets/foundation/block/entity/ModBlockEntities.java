package com.holybuckets.foundation.block.entity;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.block.ModBlocks;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlockEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static DeferredObject<BlockEntityType<SimpleBlockEntity>> simpleBlockEntity;

    public static void initialize(BalmBlockEntities blockEntities) {
        simpleBlockEntity = blockEntities.registerBlockEntity(id("simple_block_entity"), SimpleBlockEntity::new, () -> new Block[]{ModBlocks.stoneBrickBlockEntity} );
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

    private static BlockItem itemBlock(Block block) {
        return new BlockItem(block, Balm.getItems().itemProperties());
    }

}
