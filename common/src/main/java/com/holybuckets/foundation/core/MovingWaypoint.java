package com.holybuckets.foundation.core;

import com.google.gson.JsonObject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class MovingWaypoint {

    private static class Waypoint {
        String levelId;
        BlockPos targetPos;
        int colorId;

        public Waypoint(String levelId, BlockPos targetPos, int colorId) {
            this.levelId = levelId;
            this.targetPos = targetPos;
            this.colorId = colorId;
        }
    }

    public static final String MSG_ID_MOVING_WAYPOINT = "moving_waypoint";
    private static final int MAX_COLORS = 16;

    private static final Map<ServerPlayer, IntObjectMap<Waypoint>> playerWaypoints = new HashMap<>();

    public static void setWaypoint(ServerPlayer player, BlockPos target) {
        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(player, k -> new IntObjectHashMap<>());
        int nextColorId = findNextFreeColor(waypoints);
        setWaypoint(player, target, nextColorId);
    }

    public static void setWaypoint(ServerPlayer player, BlockPos target, int colorId) {
        String levelId = player.level().dimension().location().toString();

        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(player, k -> new IntObjectHashMap<>());
        waypoints.put(colorId, new Waypoint(levelId, target, colorId));

        sendWaypointToClient(player, levelId, target, colorId);
    }

    public static void removeWaypoint(ServerPlayer player, int colorId) {
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(player);
        if (waypoints != null) {
            waypoints.remove(colorId);
            sendRemoveWaypointToClient(player, colorId);
        }
    }

    public static void clearAllWaypoints(ServerPlayer player) {
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(player);
        if (waypoints != null) {
            for (IntObjectMap.PrimitiveEntry<Waypoint> entry : waypoints.entries()) {
                sendRemoveWaypointToClient(player, entry.key());
            }
            waypoints.clear();
        }
        playerWaypoints.remove(player);
    }

    public static void removePlayerData(ServerPlayer player) {
        playerWaypoints.remove(player);
    }

    private static int findNextFreeColor(IntObjectMap<Waypoint> waypoints) {
        for (int i = 0; i < MAX_COLORS; i++) {
            if (!waypoints.containsKey(i)) {
                return i;
            }
        }
        return 0;
    }

    private static void sendWaypointToClient(ServerPlayer player, String levelId, BlockPos targetPos, int colorId) {
        JsonObject json = new JsonObject();
        json.addProperty("levelId", levelId);
        json.addProperty("targetPos", HBUtil.BlockUtil.positionToString(targetPos));
        json.addProperty("colorId", colorId);

        SimpleStringMessage.createAndFire(player, MSG_ID_MOVING_WAYPOINT, json.toString());
    }

    private static void sendRemoveWaypointToClient(ServerPlayer player, int colorId) {
        JsonObject json = new JsonObject();
        json.addProperty("colorId", colorId);

        SimpleStringMessage.createAndFire(player, MSG_ID_MOVING_WAYPOINT, json.toString());
    }
}