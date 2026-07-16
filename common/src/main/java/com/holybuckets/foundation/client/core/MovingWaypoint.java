package com.holybuckets.foundation.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.core.WoolColorHelper;
import com.holybuckets.foundation.mixin.ClientLevelAccessor;
import com.holybuckets.foundation.event.custom.ClientLevelTickEvent;
import com.holybuckets.foundation.event.custom.DetermineActiveWaypointEvent;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.holybuckets.foundation.core.MovingWaypoint.MAX_COLORS;
import static com.holybuckets.foundation.core.MovingWaypoint.MSG_ID_MOVING_WAYPOINT;

public class MovingWaypoint {

    public static class Waypoint {
        public String levelId;
        public BlockPos targetPos;
        public int colorId;
        public boolean isActive;
        public int nearTicks;
        public int waypointId;
        public boolean isPermanent;
        public UUID linkedEntityUuid;
        public String nameTag;
        public BlockPos entityTargetPos;

        public static int activeCount = 0;

        public BlockPos getTargetPos() {
            return entityTargetPos != null ? entityTargetPos : targetPos;
        }

        public Waypoint(String levelId, BlockPos targetPos, int colorId) {
            this(levelId, targetPos, colorId % MAX_COLORS, colorId, false, null, null);
        }

        public Waypoint(String levelId, BlockPos targetPos, int colorId, int waypointId,
                        boolean isPermanent, UUID linkedEntityUuid, String nameTag) {
            this.levelId = levelId;
            this.colorId = colorId % MAX_COLORS;
            this.targetPos = targetPos;
            this.nearTicks = 0;
            this.waypointId = waypointId;
            this.isPermanent = isPermanent;
            this.linkedEntityUuid = linkedEntityUuid;
            this.nameTag = nameTag;
            setActive(CURRENT_LEVEL_ID);
        }

        public void setActive(String currentLevelId) {
            boolean wasActive = this.isActive;
            this.isActive = currentLevelId != null && currentLevelId.equals(this.levelId);

            if (wasActive && !this.isActive) {
                activeCount--;
            } else if (!wasActive && this.isActive) {
                activeCount++;
            }
        }

        public void deactivate() {
            if (this.isActive) {
                this.isActive = false;
                activeCount--;
            }
        }

        public static void remove(BlockPos pos) {
            String msg = "Waypoint at " + HBUtil.BlockUtil.positionToString(pos) + " removed";
            IMessager.getInstance().sendBottomActionHint(msg);
        }
    }

    public static String CURRENT_LEVEL_ID = "";
    private static final IntObjectMap<Waypoint> originalWaypoints = new IntObjectHashMap<>();
    private static final IntObjectMap<Waypoint> activeWaypoints = new IntObjectHashMap<>();
    private static BufferBuilder bufferBuilder = null;
    private static final int MAX_BEACON_VERTICES = 256 * 1024;
    private static final int MAX_CONCURRENT_BEACONS = 8;
    private static final int MAX_RANGE = 512;
    // Beam (core) radius — kept constant in world units, slightly larger than vanilla beacons.
    private static final float BEAM_RADIUS = 0.35f;
    // Glow (outer halo) radius — base in world units; scaled by camera distance / GLOW_SCALE_REF
    // so the glow stays angularly readable from far away.
    private static final float GLOW_RADIUS_BASE = 0.45f;
    private static final float GLOW_SCALE_REF = 24.0f;
    private static final int DELETE_NEAR_HORIZ_DIST = 4;
    private static final int DELETE_NEAR_TICKS_THRESHOLD = 60; // ~3 sec at 20 tps
    // Query cadence for the moving waypoint position recompute (ON_20_TICKS = 1 sec).
    private static final int TICK_CADENCE = 20;

    private final UUID playerId;
    private final String waypointKey;
    private BlockPos targetPosition;
    private BlockPos waypointPosition;
    private int colorId;

    public MovingWaypoint(Player player, BlockPos targetPosition, int colorId) {
        this.playerId = player.getUUID();
        this.waypointKey = "moving_waypoint_" + System.currentTimeMillis();
        this.targetPosition = targetPosition;
        this.colorId = colorId;
        this.waypointPosition = calculateWaypoint(player);
        setWaypointFlare();
    }

    public void setTargetPosition(Player player, BlockPos newTarget) {
        this.targetPosition = newTarget;
        this.waypointPosition = calculateWaypoint(player);
        setWaypointFlare();
    }

    public BlockPos getWaypointPosition() {
        return waypointPosition;
    }

    public BlockPos getTargetPosition() {
        return targetPosition;
    }

    public void updateWaypoint(Player player) {
        BlockPos oldWaypoint = this.waypointPosition;
        this.waypointPosition = calculateWaypoint(player);

        if (!oldWaypoint.equals(this.waypointPosition)) {
            setWaypointFlare();
        }
    }

    private void setWaypointFlare() {
        // Remove existing active waypoint for this color
        activeWaypoints.remove(colorId);
        
        // Add new active waypoint at calculated position
        activeWaypoints.put(colorId, new Waypoint(CURRENT_LEVEL_ID, waypointPosition, colorId));
    }

    public void clearWaypoint() {
        // Remove from both original and active waypoints
        Waypoint originalWp = originalWaypoints.get(colorId);
        if (originalWp != null) {
            originalWp.deactivate();
            originalWaypoints.remove(colorId);
        }
        
        Waypoint activeWp = activeWaypoints.get(colorId);
        if (activeWp != null) {
            activeWp.deactivate();
            activeWaypoints.remove(colorId);
        }
    }

    private BlockPos calculateWaypoint(Player player) {
        Vec3 playerPos = player.position();
        Vec3 targetPos = Vec3.atCenterOf(targetPosition);

        double horizDistSq = horizontalDistanceSq(playerPos, targetPos);
        double maxRangeSq = (double) MAX_RANGE * MAX_RANGE;

        if (horizDistSq <= maxRangeSq) {
            return targetPosition;
        }

        double horizDist = Math.sqrt(horizDistSq);
        double scale = MAX_RANGE / horizDist;
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        return BlockPos.containing(
            playerPos.x + dx * scale,
            targetPos.y,
            playerPos.z + dz * scale
        );
    }

    public boolean isInRange(Player player) {
        return horizontalDistance(player.position(), Vec3.atCenterOf(targetPosition)) <= MAX_RANGE;
    }

    public double getDistanceToTarget(Player player) {
        return horizontalDistance(player.position(), Vec3.atCenterOf(targetPosition));
    }

    public double getDistanceToWaypoint(Player player) {
        return horizontalDistance(player.position(), Vec3.atCenterOf(waypointPosition));
    }

    // Horizontal (xz-only) distance helpers — Y is ignored so that targets which the
    // server may have floor-anchored (Y = minBuildHeight) don't blow up the range math.
    private static double horizontalDistanceSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.sqrt(horizontalDistanceSq(a, b));
    }

    public static void clearAllWaypoints(UUID playerId) {
        // Clear all waypoints (this method might need to be redesigned if we need per-player tracking)
        for (Waypoint wp : originalWaypoints.values()) {
            wp.deactivate();
        }
        for (Waypoint wp : activeWaypoints.values()) {
            wp.deactivate();
        }
        originalWaypoints.clear();
        activeWaypoints.clear();
    }


    private static void determineActiveWaypointHook(Waypoint wp, Player player) {
        ClientEventRegistrar.getInstance().onDetermineActiveWaypoint(
            new DetermineActiveWaypointEvent(wp, player));
    }

    public static void updateAllActiveWaypoints(Player player) {
        // Update active waypoints based on original waypoints and player position.
        // Range/projection use xz-only distance; the target's Y is preserved on the active waypoint.
        activeWaypoints.clear();


        LevelEntityGetter<Entity> entityGetter = null;
        if (player.level() instanceof ClientLevel cl) {
            entityGetter = ((ClientLevelAccessor)(Object) cl).getEntityGetter();
        }

        double maxRangeSq = (double) MAX_RANGE * MAX_RANGE;
        for (IntObjectMap.PrimitiveEntry<Waypoint> entry : originalWaypoints.entries())
        {
            int wpId = entry.key();
            Waypoint originalWp = entry.value();
            originalWp.setActive(CURRENT_LEVEL_ID);

            MovingWaypoint.determineActiveWaypointHook(originalWp, player);

            if(!originalWp.isActive) continue;

            if (originalWp.linkedEntityUuid != null && entityGetter != null) {
                Entity ent = entityGetter.get(originalWp.linkedEntityUuid);
                originalWp.entityTargetPos = (ent != null && !ent.isRemoved()) ? ent.blockPosition() : null;
            } else {
                originalWp.entityTargetPos = null;
            }

            Vec3 playerPos = player.position();
            Vec3 targetPos = Vec3.atCenterOf(originalWp.getTargetPos());
            double horizDistSq = horizontalDistanceSq(playerPos, targetPos);

            BlockPos waypointPos;
            if (horizDistSq <= maxRangeSq) {
                waypointPos = originalWp.getTargetPos();
            } else {
                double horizDist = Math.sqrt(horizDistSq);
                double scale = MAX_RANGE / horizDist;
                double dx = targetPos.x - playerPos.x;
                double dz = targetPos.z - playerPos.z;
                waypointPos = BlockPos.containing(
                    playerPos.x + dx * scale,
                    targetPos.y,
                    playerPos.z + dz * scale
                );
            }

            activeWaypoints.put(wpId, new Waypoint(CURRENT_LEVEL_ID, waypointPos, originalWp.colorId));
        }
    }

    //** EVENTS

    public static void registerEvents(ClientEventRegistrar registrar ) {
        registrar.registerOnSimpleMessage(MSG_ID_MOVING_WAYPOINT, MovingWaypoint::onMovingWaypointMessage);
        registrar.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, MovingWaypoint::tryRenderWaypointFlare);
        registrar.registerOnClientLevelTick(TickType.ON_20_TICKS, MovingWaypoint::onClient20Tick);
        // Eagerly seed CURRENT_LEVEL_ID at login so waypoint messages that arrive before
        // the first 120-tick fires aren't dropped as inactive.
        registrar.registerOnConnectedToServer(MovingWaypoint::onConnectedToServer);
    }

    private static void onConnectedToServer(ConnectedToServerEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            CURRENT_LEVEL_ID = HBUtil.LevelUtil.toLevelIdAgnostic(level);
        }
    }

    private static void onMovingWaypointMessage(SimpleMessageEvent event)
    {
        JsonElement json = JsonParser.parseString(event.getContent());
        if (json.isJsonNull() || !json.isJsonObject()) return;
        JsonObject obj = json.getAsJsonObject();

        int colorId = 0;
        if (obj.has("colorId"))
            colorId = obj.get("colorId").getAsInt();

        int waypointId = colorId;
        if (obj.has("waypointId"))
            waypointId = obj.get("waypointId").getAsInt();

        if (!obj.has("levelId") || !obj.has("targetPos")) {
            // Remove waypoint
            Waypoint originalWp = originalWaypoints.get(waypointId);
            if (originalWp != null) {
                originalWp.deactivate();
                originalWaypoints.remove(waypointId);
            }
            activeWaypoints.remove(waypointId);
            return;
        }

        boolean isPermanent = obj.has("isPermanent") && obj.get("isPermanent").getAsBoolean();
        UUID linkedEntityUuid = null;
        if (obj.has("linkedEntityUuid")) {
            try {
                linkedEntityUuid = UUID.fromString(obj.get("linkedEntityUuid").getAsString());
            } catch (IllegalArgumentException ignored) {
                // malformed uuid → treat as unset
            }
        }
        String nameTag = (obj.has("nameTag") && !obj.get("nameTag").isJsonNull())
            ? obj.get("nameTag").getAsString() : null;

        // Add or update original waypoint
        Waypoint w = new Waypoint(
            obj.get("levelId").getAsString(),
            HBUtil.BlockUtil.stringToBlockPos(obj.get("targetPos").getAsString()),
            colorId,
            waypointId,
            isPermanent,
            linkedEntityUuid,
            nameTag
        );

        originalWaypoints.put(waypointId, w);
        
        // Update active waypoints if player is available
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            updateAllActiveWaypoints(player);
        }
    }

    private static void onClient20Tick(ClientLevelTickEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            CURRENT_LEVEL_ID = HBUtil.LevelUtil.toLevelIdAgnostic(level);
        }

        Player player = Minecraft.getInstance().player;
        if (player == null || originalWaypoints.isEmpty()) return;

        updateAllActiveWaypoints(player);

        // Dwell-then-delete: if the player stays within DELETE_NEAR_HORIZ_DIST blocks
        // (xz only) of an original target for DELETE_NEAR_TICKS_THRESHOLD ticks, clear it.
        Vec3 playerPos = player.position();
        double nearDistSq = (double) DELETE_NEAR_HORIZ_DIST * DELETE_NEAR_HORIZ_DIST;

        java.util.List<Integer> toRemove = null;
        for (IntObjectMap.PrimitiveEntry<Waypoint> entry : originalWaypoints.entries()) {
            Waypoint wp = entry.value();
            if (!wp.isActive) {
                wp.nearTicks = 0;
                continue;
            }
            if (wp.isPermanent) {
                wp.nearTicks = 0;
                continue;
            }
            Vec3 wpPos = Vec3.atCenterOf(wp.getTargetPos());
            if (horizontalDistanceSq(playerPos, wpPos) <= nearDistSq) {
                wp.nearTicks += TICK_CADENCE;
                if (wp.nearTicks >= DELETE_NEAR_TICKS_THRESHOLD) {
                    if (toRemove == null) toRemove = new java.util.ArrayList<>();
                    toRemove.add(entry.key());
                }
            } else {
                wp.nearTicks = 0;
            }
        }

        if (toRemove != null) {
            for (int waypointId : toRemove) {
                Waypoint removed = originalWaypoints.get(waypointId);
                if (removed != null) {
                    removed.deactivate();
                    originalWaypoints.remove(waypointId);
                    Waypoint.remove(removed.getTargetPos());
                }
                activeWaypoints.remove(waypointId);
            }
        }
    }

    //** RENDERING

    private static void tryRenderWaypointFlare(RenderLevelEvent event) {
        try {
            renderWaypointFlare(event);
        } catch (Exception ex) {
            bufferBuilder = null;
            String msg = "MovingWaypoint: Error rendering waypoint flare visuals, resetting buffer. " +
                "This is not a critical error but let the author know if it happens repeatedly error:\n" + ex.getMessage();
            LoggerBase.logWarning(null,"007000", msg);
        }
    }

    private static void renderWaypointFlare(RenderLevelEvent event) {
        if (activeWaypoints.isEmpty()) return;

        PoseStack poseStack = new PoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        long gameTime = Minecraft.getInstance().level.getGameTime();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
            .renderBuffers().bufferSource();


        // Push fog far out so beams remain visible past the world's fog cutoff.
        // Restored in the finally block below.
        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

        try {
            int renderedCount = 0;
            for (IntObjectMap.PrimitiveEntry<Waypoint> entry : activeWaypoints.entries()) {
                Waypoint wp = entry.value();

                if (!wp.isActive) continue;
                if (renderedCount >= MAX_CONCURRENT_BEACONS) break;

                BlockPos targetPos = wp.getTargetPos();

                // Distance from camera to the beam base (xz-aware, but Y matters here for
                // angular sizing of the halo when the camera is well above/below the floor-
                // anchored target).
                double cameraDist = cameraPos.distanceTo(Vec3.atCenterOf(targetPos));
                float glowRadius = (float) Math.max(GLOW_RADIUS_BASE,
                    GLOW_RADIUS_BASE * (cameraDist / GLOW_SCALE_REF));

                poseStack.pushPose();

                poseStack.translate(
                    targetPos.getX() - cameraPos.x + 0.5,
                    targetPos.getY() - cameraPos.y,
                    targetPos.getZ() - cameraPos.z + 0.5
                );

                int colors = WoolColorHelper.getWoolColorRGBInt(wp.colorId);

                BeaconRenderer.renderBeaconBeam(
                    poseStack,
                    bufferSource,
                    BeaconRenderer.BEAM_LOCATION,
                    event.getPartialTick(),
                    1.0f,
                    gameTime,
                    0,
                    Minecraft.getInstance().level.getMaxBuildHeight() - targetPos.getY(),
                    colors,
                    BEAM_RADIUS,
                    glowRadius
                );

                poseStack.popPose();
                renderedCount++;
            }
        } finally {
            RenderSystem.setShaderFogStart(prevFogStart);
            RenderSystem.setShaderFogEnd(prevFogEnd);
        }
    }
}
