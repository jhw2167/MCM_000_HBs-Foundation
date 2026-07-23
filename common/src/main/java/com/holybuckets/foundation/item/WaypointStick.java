package com.holybuckets.foundation.item;

import com.holybuckets.foundation.core.MovingWaypoint;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.VanillaEntityLike;
import com.holybuckets.foundation.event.balm.LivingDamageEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
*
* Sets a waypoint on entities when struck. Right click to remove
*/
public class WaypointStick extends Item {

    private static final double DELETE_NEAR_HORIZ_DIST = 16.0;

    // Shared across every WaypointStick in the world. Starts at MAX_COLORS so the
    // first use yields colorId 0 (16 % 16), cleanly cycling through wool colors.
    private static int currentStickId = MovingWaypoint.MAX_COLORS;

    public WaypointStick(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerDamage(WaypointStick::livingEntityHurt);
    }

    private static void livingEntityHurt(LivingDamageEvent dmgEvent) {
        Entity target = dmgEvent.getEntity();
        if(target.level().isClientSide()) return;
        Entity p = dmgEvent.getDamageSource().getEntity();
        if(!(p instanceof ServerPlayer sp)) return;
        if(!(sp.getMainHandItem().getItem() instanceof WaypointStick)) return;

        StickIds ids = nextStickIds();
        String nameTag = target.getDisplayName() != null ? target.getDisplayName().getString() : null;
        MovingWaypoint.setWaypoint(sp, target.blockPosition(), ids.colorId, ids.waypointId,
            false, new VanillaEntityLike(target), nameTag);
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

    /**
     * Right-click into the air (no block / entity under the crosshair). Deletes the
     * nearest waypoint within {@link #DELETE_NEAR_HORIZ_DIST} blocks (xz only).
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        Vec3 pos = sp.position();
        MovingWaypoint.WaypointInfo nearest =
            MovingWaypoint.getNearestWaypoint(sp, pos, DELETE_NEAR_HORIZ_DIST);
        if (nearest != null) {
            // Remove by the map-key (colorId) since that's the canonical existing remove.
            MovingWaypoint.removeWaypoint(sp, nearest.colorId);
        }
        return InteractionResult.SUCCESS;
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
