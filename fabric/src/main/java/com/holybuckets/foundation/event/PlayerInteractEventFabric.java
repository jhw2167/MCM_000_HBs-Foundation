package com.holybuckets.foundation.event;

import com.holybuckets.foundation.event.custom.PlayerInteractEvent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerInteractEventFabric {

    public static void register() {
        UseItemCallback.EVENT.register(PlayerInteractEventFabric::onUseItem);
        UseBlockCallback.EVENT.register(PlayerInteractEventFabric::onUseBlock);
        UseEntityCallback.EVENT.register(PlayerInteractEventFabric::onUseEntity);
    }

    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean canceled = EventRegistrar.getInstance().onPlayerInteract(
            new PlayerInteractEvent.RightClickInteraction(player, level, hand, stack, null, null)
        );
        return canceled ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        boolean canceled = EventRegistrar.getInstance().onPlayerInteract(
            new PlayerInteractEvent.RightClickInteraction(
                player, level, hand, stack,
                hitResult != null ? hitResult.getBlockPos() : null,
                hitResult != null ? hitResult.getDirection() : null
            )
        );
        return canceled ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 localPos = hitResult != null ? hitResult.getLocation() : null;
        boolean canceled = EventRegistrar.getInstance().onPlayerInteract(
            new PlayerInteractEvent.EntityInteract(
                player, level, hand, stack, null, null, entity, localPos
            )
        );
        return canceled ? InteractionResult.FAIL : InteractionResult.PASS;
    }
}
