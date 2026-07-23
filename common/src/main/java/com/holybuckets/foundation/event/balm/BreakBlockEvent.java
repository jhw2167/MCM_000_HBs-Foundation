package com.holybuckets.foundation.event.balm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

public class BreakBlockEvent extends BalmEvent {
    private final LevelAccessor level;
    private final Player player;
    private final BlockPos pos;
    private final BlockState state;
    private final BlockEntity blockEntity;

    public BreakBlockEvent(LevelAccessor level, @Nullable Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        this.level = level;
        this.player = player;
        this.pos = pos;
        this.state = state;
        this.blockEntity = blockEntity;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
