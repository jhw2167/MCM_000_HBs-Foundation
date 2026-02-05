package com.holybuckets.foundation.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.console.IMessager;
import com.holybuckets.foundation.event.custom.ClientLevelTickEvent;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
            this.targetPos = targetPos;
            this.colorId = colorId;
            setActive(CURRENT_LEVEL_ID, Minecraft.getInstance().player);
        }

        public void setActive(String currentLevelId, Player p)
        {
            this.isActive = false;
            if(!currentLevelId.equals(this.levelId) ) return;
            this.isActive = true; activeCount++;
        }

        public static void remove(BlockPos pos) {
            String msg = "Waypoint at " + HBUtil.BlockUtil.positionToString(pos) + " removed";
            IMessager.getInstance().sendBottomActionHint(msg);
        }

    }

    public static String CURRENT_LEVEL_ID = "";
    private static final IntObjectMap<Waypoint> activeWaypoints = new IntObjectHashMap<>();
    private static BufferBuilder bufferBuilder = null;
    private static final int MAX_BEACON_VERTICES = 256 * 1024;
    private static final int MAX_CONCURRENT_BEACONS = 8;
    private static final int MAX_RANGE = 256;

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
        activeWaypoints.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(waypointKey, new Waypoint(waypointPosition, colorId));
    }

    public void clearWaypoint() {
        Map<String, Waypoint> playerWaypoints = activeWaypoints.get(playerId);
        if (playerWaypoints != null) {
            Waypoint wp = playerWaypoints.get(waypointKey);
            if (wp != null) {
                wp.deactivate();
                playerWaypoints.remove(waypointKey);
            }
            if (playerWaypoints.isEmpty()) {
                activeWaypoints.remove(playerId);
            }
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
        Map<String, Waypoint> playerWaypoints = activeWaypoints.get(playerId);
        if (playerWaypoints != null) {
            playerWaypoints.values().forEach(Waypoint::deactivate);
            activeWaypoints.remove(playerId);
        }
    }

    //** EVENTS

    public static final String MSG_ID_MOVING_WAYPOINT = "moving_waypoint";
    public static void registerEvents(ClientEventRegistrar registrar ) {
        registrar.registerOnSimpleMessage(MSG_ID_MOVING_WAYPOINT, MovingWaypoint::onMovingWaypointMessage);
            registrar.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_PARTICLES, MovingWaypoint::tryRenderWaypointFlare);

    }

    private static void onMovingWaypointMessage(SimpleMessageEvent event)
    {
        JsonElement json = JsonParser.parseString( event.getContent() );
        if(json.isJsonNull() || !json.isJsonObject()) return;
        JsonObject obj = json.getAsJsonObject();

        int colorId = 0;
        if( obj.has("colorId") )
            colorId = obj.get("colorId").getAsInt();

        if(!obj.has("levelId") || !obj.has("targetPos")) {
            activeWaypoints.remove( colorId );
            return;
        }

        Waypoint w = new Waypoint(
            obj.get("levelId").getAsString(),
            HBUtil.BlockUtil.stringToBlockPos( obj.get("targetPos").getAsString() ),
            colorId
        );
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

        for (var waypoints : activeWaypoints.values()) {
            for (var wp : waypoints.values()) {

                if (!wp.isActive) continue;
                if (Waypoint.activeCount > MAX_CONCURRENT_BEACONS) {
                    if (Math.random() > ((double) MAX_CONCURRENT_BEACONS / (double) Waypoint.activeCount)) {
                        continue;
                    }
                }
                BlockPos targetPos = wp.targetPos;

                poseStack.pushPose();

                poseStack.translate(
                    targetPos.getX() - cameraPos.x + 0.5,
                    targetPos.getY() - cameraPos.y,
                    targetPos.getZ() - cameraPos.z + 0.5
                );

                float[] colors = WoolDustHelper.getWoolColorRGB(wp.colorId);

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
                    0.2f,
                    0.25f
                );

                poseStack.popPose();
            }
        }
    }
}