package com.holybuckets.foundation.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.model.EntityLike;
import com.holybuckets.foundation.model.EntityLikeResolver;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.core.WoolColorHelper;
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
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
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

    // Update active waypoints based on original waypoints and player position.
    // Range/projection use xz-only distance; the target's Y is preserved on the active waypoint.
    public static void updateAllActiveWaypoints(Player player) {

        activeWaypoints.clear();

        double maxRangeSq = (double) MAX_RANGE * MAX_RANGE;
        for (IntObjectMap.PrimitiveEntry<Waypoint> entry : originalWaypoints.entries())
        {
            int wpId = entry.key();
            Waypoint originalWp = entry.value();
            originalWp.setActive(CURRENT_LEVEL_ID);

            MovingWaypoint.determineActiveWaypointHook(originalWp, player);

            if(!originalWp.isActive) continue;

            if (originalWp.linkedEntityUuid != null) {
                Optional<EntityLike> resolved =
                    EntityLikeResolver.resolveEntity(originalWp.linkedEntityUuid, player.level());
                originalWp.entityTargetPos =
                    (resolved.isPresent() && resolved.get().isValid()) ? resolved.get().blockPosition() : null;
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
                    player.level().getMinBuildHeight()+1,
                    playerPos.z + dz * scale
                );
            }

            activeWaypoints.put(wpId, new Waypoint(CURRENT_LEVEL_ID, waypointPos, originalWp.colorId));
        }
    }

    //** EVENTS

    public static void registerEvents(ClientEventRegistrar registrar ) {
        EntityLikeResolver.register(ClientEntityLikeResolver.INSTANCE);
        registrar.registerOnSimpleMessage(MSG_ID_MOVING_WAYPOINT, MovingWaypoint::onMovingWaypointMessage);
        registrar.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, MovingWaypoint::tryRenderWaypointFlare);
        registrar.registerOnClientLevelTick(TickType.ON_20_TICKS, MovingWaypoint::onClient20Tick);
        registrar.registerOnDisconnectedFromServer(MovingWaypoint::onConnectedToServer);
    }

    private static void onConnectedToServer(DisconnectedFromServerEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            CURRENT_LEVEL_ID = HBUtil.LevelUtil.toLevelIdAgnostic(level);
        }
        //clear waypoints
       originalWaypoints.clear();
       activeWaypoints.clear();
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

        if (bufferBuilder == null) {
            bufferBuilder = new BufferBuilder(MAX_BEACON_VERTICES);
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        long gameTime = Minecraft.getInstance().level.getGameTime();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
            .renderBuffers().bufferSource();

        // Push fog far out so beams remain visible past the world's fog cutoff.
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

                // Modulate beam distance
                double cameraDist = cameraPos.distanceTo(Vec3.atCenterOf(targetPos));
                float glowRadius = (float) Math.max(GLOW_RADIUS_BASE,
                    GLOW_RADIUS_BASE * (cameraDist / GLOW_SCALE_REF));

                poseStack.pushPose();

                poseStack.translate(
                    targetPos.getX() - cameraPos.x + 0.5,
                    targetPos.getY() - cameraPos.y,
                    targetPos.getZ() - cameraPos.z + 0.5
                );

                float[] colors = WoolColorHelper.getWoolColorRGB(wp.colorId);

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
