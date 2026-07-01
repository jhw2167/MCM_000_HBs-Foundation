package com.holybuckets.foundation.event.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class PlayerInteractEvent {

    private final Player player;
    private final Level level;
    private final InteractionHand hand;
    private final ItemStack itemStack;
    @Nullable private final BlockPos pos;
    @Nullable private final Direction face;
    private boolean canceled;

    public PlayerInteractEvent(Player player, Level level, InteractionHand hand, ItemStack itemStack,
                               @Nullable BlockPos pos, @Nullable Direction face) {
        this.player = player;
        this.level = level;
        this.hand = hand;
        this.itemStack = itemStack;
        this.pos = pos;
        this.face = face;
    }

    public Player getPlayer() { return player; }
    public Level getLevel() { return level; }
    public InteractionHand getHand() { return hand; }
    public ItemStack getItemStack() { return itemStack; }
    @Nullable public BlockPos getPos() { return pos; }
    @Nullable public Direction getFace() { return face; }

    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }


    public static class LeftClickInteraction extends PlayerInteractEvent {
        public LeftClickInteraction(Player player, Level level, InteractionHand hand, ItemStack itemStack,
                                    @Nullable BlockPos pos, @Nullable Direction face) {
            super(player, level, hand, itemStack, pos, face);
        }
    }


    public static class RightClickInteraction extends PlayerInteractEvent {
        public RightClickInteraction(Player player, Level level, InteractionHand hand, ItemStack itemStack,
                                     @Nullable BlockPos pos, @Nullable Direction face) {
            super(player, level, hand, itemStack, pos, face);
        }
    }


    public static class EntityInteract extends PlayerInteractEvent {
        private final Entity target;
        private final Vec3 localPos;

        public EntityInteract(Player player, Level level, InteractionHand hand, ItemStack itemStack,
                              @Nullable BlockPos pos, @Nullable Direction face,
                              Entity target, Vec3 localPos) {
            super(player, level, hand, itemStack, pos, face);
            this.target = target;
            this.localPos = localPos;
        }

        public Entity getTarget() { return target; }
        public Vec3 getLocalPos() { return localPos; }
    }
}
