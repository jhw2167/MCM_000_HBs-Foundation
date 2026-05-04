package com.holybuckets.foundation.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.core.WoolColorHelper;
import com.holybuckets.foundation.event.custom.ClientLevelTickEvent;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MovingWaypoint {

    private static class Waypoint {
        String levelId;
        BlockPos targetPos;
        int colorId;
        boolean isActive;

        public static int activeCount = 0;

        public Waypoint(String levelId, BlockPos targetPos, int colorId) {
            this.levelId = levelId;
            this.colorId = colorId;
            this.targetPos = targetPos;
            setActive(CURRENT_LEVEL_ID);
        }

        public void setActive(String currentLevelId) {
            boolean wasActive = this.isActive;
            this.isActive = currentLevelId.equals(this.levelId);

            if(isActive) {
                Level level = Minecraft.getInstance().level;
               targetPos = targetPos.atY(level.getMinBuildHeight());
            }
            
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
    private static final int MAX_RANGE = 192;

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

        double distanceToTarget = playerPos.distanceTo(targetPos);

        if (distanceToTarget <= MAX_RANGE) {
            return targetPosition;
        }

        Vec3 direction = targetPos.subtract(playerPos).normalize();
        Vec3 waypoint = playerPos.add(direction.scale(MAX_RANGE));

        return BlockPos.containing(waypoint.x, waypoint.y, waypoint.z);
    }

    public boolean isInRange(Player player) {
        return player.position().distanceTo(Vec3.atCenterOf(targetPosition)) <= MAX_RANGE;
    }

    public double getDistanceToTarget(Player player) {
        return player.position().distanceTo(Vec3.atCenterOf(targetPosition));
    }

    public double getDistanceToWaypoint(Player player) {
        return player.position().distanceTo(Vec3.atCenterOf(waypointPosition));
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

    public static void updateAllActiveWaypoints(Player player) {
        // Update active waypoints based on original waypoints and player position
        activeWaypoints.clear();
        
        for (IntObjectMap.PrimitiveEntry<Waypoint> entry : originalWaypoints.entries())
        {
            int colorId = entry.key();
            Waypoint originalWp = entry.value();
            originalWp.setActive(CURRENT_LEVEL_ID);
            
            if(!originalWp.isActive) continue;
            
            Vec3 playerPos = player.position();
            Vec3 targetPos = Vec3.atCenterOf(originalWp.targetPos);
            double distanceToTarget = playerPos.distanceTo(targetPos);
            
            BlockPos waypointPos;
            if (distanceToTarget <= MAX_RANGE) {
                waypointPos = originalWp.targetPos;
            } else {
                Vec3 direction = targetPos.subtract(playerPos).normalize();
                Vec3 waypoint = playerPos.add(direction.scale(MAX_RANGE));
                waypointPos = BlockPos.containing(waypoint.x, waypoint.y, waypoint.z);
            }
            
            activeWaypoints.put(colorId, new Waypoint(CURRENT_LEVEL_ID, waypointPos, colorId));
        }
    }

    //** EVENTS

    public static final String MSG_ID_MOVING_WAYPOINT = "moving_waypoint";
    public static void registerEvents(ClientEventRegistrar registrar ) {
        registrar.registerOnSimpleMessage(MSG_ID_MOVING_WAYPOINT, MovingWaypoint::onMovingWaypointMessage);
        registrar.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, MovingWaypoint::tryRenderWaypointFlare);
        registrar.registerOnClientLevelTick(TickType.ON_120_TICKS, MovingWaypoint::onClient120Tick);
    }

    private static void onMovingWaypointMessage(SimpleMessageEvent event)
    {
        JsonElement json = JsonParser.parseString(event.getContent());
        if (json.isJsonNull() || !json.isJsonObject()) return;
        JsonObject obj = json.getAsJsonObject();

        int colorId = 0;
        if (obj.has("colorId"))
            colorId = obj.get("colorId").getAsInt();

        if (!obj.has("levelId") || !obj.has("targetPos")) {
            // Remove waypoint
            Waypoint originalWp = originalWaypoints.get(colorId);
            if (originalWp != null) {
                originalWp.deactivate();
                originalWaypoints.remove(colorId);
            }
            activeWaypoints.remove(colorId);
            return;
        }

        // Add or update original waypoint
        Waypoint w = new Waypoint(
            obj.get("levelId").getAsString(),
            HBUtil.BlockUtil.stringToBlockPos(obj.get("targetPos").getAsString()),
            colorId
        );
        
        originalWaypoints.put(colorId, w);
        
        // Update active waypoints if player is available
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            updateAllActiveWaypoints(player);
        }
    }

    private static void onClient120Tick(ClientLevelTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && !originalWaypoints.isEmpty()) {
            updateAllActiveWaypoints(player);
        }
        CURRENT_LEVEL_ID = HBUtil.LevelUtil.toLevelIdAgnostic(Minecraft.getInstance().level);
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

        int renderedCount = 0;
        for (IntObjectMap.PrimitiveEntry<Waypoint> entry : activeWaypoints.entries()) {
            Waypoint wp = entry.value();

            if (!wp.isActive) continue;
            if (renderedCount >= MAX_CONCURRENT_BEACONS) break;
            
            BlockPos targetPos = wp.targetPos;

            poseStack.pushPose();

            poseStack.translate(
                targetPos.getX() - cameraPos.x + 0.5,
                targetPos.getY() - cameraPos.y,
                targetPos.getZ() - cameraPos.z + 0.5
            );

            float[] colors = WoolColorHelper.getWoolColorRGB(wp.colorId);
            int color = WoolColorHelper.getWoolColorRGBInt(wp.colorId);

            BeaconRenderer.renderBeaconBeam(
                poseStack,
                bufferSource,
                BeaconRenderer.BEAM_LOCATION,
                event.getPartialTick(),
                1.0f,
                gameTime,
                0,
                Minecraft.getInstance().level.getMaxBuildHeight() - targetPos.getY(),
                color,
                0.2f,
                0.25f
            );

            poseStack.popPose();
            renderedCount++;
        }
    }
}
