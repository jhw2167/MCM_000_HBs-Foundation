package com.holybuckets.foundation.item;

import com.holybuckets.foundation.core.MovingWaypoint;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Waypoint Stick — debug tool for waypoints.
 *
 * <ul>
 *   <li>Right-click a block: set a positional waypoint at the block hit pos.</li>
 *   <li>Right-click an entity: set an entity-linked waypoint that tracks the entity.</li>
 *   <li>Right-click into the air: delete the nearest waypoint within
 *       {@link #DELETE_NEAR_HORIZ_DIST} blocks (xz only), if any.</li>
 * </ul>
 *
 * Waypoints created by the stick use their own id space. A shared static counter
 * starts at {@link MovingWaypoint#MAX_COLORS} and increments on every use; the wool
 * colorId is {@code counter % MAX_COLORS} and the waypointId is {@code colorId * 2}.
 * The {@code *2} reserves even ids for stick waypoints so they don't collide with
 * other systems writing waypoints at the same colorId.
 */
public class WaypointStick extends Item {

    private static final double DELETE_NEAR_HORIZ_DIST = 16.0;

    // Shared across every WaypointStick in the world. Starts at MAX_COLORS so the
    // first use yields colorId 0 (16 % 16), cleanly cycling through wool colors.
    private static int currentStickId = MovingWaypoint.MAX_COLORS;

    public WaypointStick() {
        super(Balm.getItems().itemProperties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        // All waypoint state lives server-side; let the client just play the swing.
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        BlockPos hit = context.getClickedPos();
        StickIds ids = nextStickIds();
        MovingWaypoint.setWaypoint(sp, hit, ids.colorId, ids.waypointId, false, null, null);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (target == null) return InteractionResult.PASS;

        StickIds ids = nextStickIds();
        String nameTag = target.getDisplayName() != null ? target.getDisplayName().getString() : null;
        MovingWaypoint.setWaypoint(sp, target.blockPosition(), ids.colorId, ids.waypointId,
            false, target, nameTag);
        return InteractionResult.SUCCESS;
    }

    /**
     * Right-click into the air (no block / entity under the crosshair). Deletes the
     * nearest waypoint within {@link #DELETE_NEAR_HORIZ_DIST} blocks (xz only).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        Vec3 pos = sp.position();
        MovingWaypoint.WaypointInfo nearest =
            MovingWaypoint.getNearestWaypoint(sp, pos, DELETE_NEAR_HORIZ_DIST);
        if (nearest != null) {
            // Remove by the map-key (colorId) since that's the canonical existing remove.
            MovingWaypoint.removeWaypoint(sp, nearest.colorId);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Allocate the next (colorId, waypointId) pair from the shared counter. */
    private static StickIds nextStickIds() {
        int colorId = currentStickId % MovingWaypoint.MAX_COLORS;
        int waypointId = colorId * 2;
        currentStickId++;
        return new StickIds(colorId, waypointId);
    }

    private static final class StickIds {
        final int colorId;
        final int waypointId;

        StickIds(int colorId, int waypointId) {
            this.colorId = colorId;
            this.waypointId = waypointId;
        }
    }
}
