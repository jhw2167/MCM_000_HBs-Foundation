package com.holybuckets.foundation.block.entity;

import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.util.DeferredObject;
import net.blay09.mods.balm.world.level.block.entity.BalmBlockEntityTypeRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static Holder<BlockEntityType<SimpleBlockEntity>> simpleBlockEntityType;
    public static DeferredObject<BlockEntityType<SimpleBlockEntity>> simpleBlockEntity;

    public static void initialize(BalmBlockEntityTypeRegistrar blockEntities) {
        simpleBlockEntityType = blockEntities.register("simple_block_entity", SimpleBlockEntity::new, ModBlocks.stoneBrickBlock).asHolder();
        simpleBlockEntity = DeferredObject.of(simpleBlockEntityType);
    }

}
