package com.holybuckets.foundation.block;

import net.blay09.mods.balm.api.Balm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class MarkerBlock extends Block {

    private final DyeColor color;

    public MarkerBlock(DyeColor color) {
        super(Balm.getBlocks().blockProperties()
            .mapColor(color)
            .sound(SoundType.STONE)
            .strength(-1.0F, 3600000.0F)
            .noLootTable());
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    public static boolean isMarker(BlockState state) {
        return state != null && state.getBlock() instanceof MarkerBlock;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.hbs_foundation.marker_block.desc").withStyle(ChatFormatting.GRAY));
    }
}
