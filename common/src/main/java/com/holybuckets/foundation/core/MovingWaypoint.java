package com.holybuckets.foundation.core;

import com.google.gson.JsonObject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

import static com.holybuckets.foundation.HBUtil.PlayerUtil;

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

    // Keyed by PlayerUtil.getId(player) so the entry survives reconnects, dim
    // changes, and respawns (which all replace the ServerPlayer instance).
    private static final Map<String, IntObjectMap<Waypoint>> playerWaypoints = new HashMap<>();

    public static void setWaypoint(ServerPlayer player, BlockPos target) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());
        int nextColorId = findNextFreeColor(waypoints);
        setWaypoint(player, target, nextColorId);
    }

    public static void setWaypoint(ServerPlayer player, BlockPos target, int colorId) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        String levelId = player.level().dimension().location().toString();

        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());
        waypoints.put(colorId, new Waypoint(levelId, target, colorId));

        sendWaypointToClient(playerId, levelId, target, colorId);
    }

    public static void removeWaypoint(ServerPlayer player, int colorId) {
        removeWaypoint(PlayerUtil.getId(player), colorId);
    }

    // String-id overload so callers tracking ids directly (e.g. SatelliteWeaponManager)
    // don't need to round-trip through a live ServerPlayer.
    public static void removeWaypoint(String playerId, int colorId) {
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints != null) {
            waypoints.remove(colorId);
            sendRemoveWaypointToClient(playerId, colorId);
        }
    }

    public static void clearAllWaypoints(ServerPlayer player) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints != null) {
            for (IntObjectMap.PrimitiveEntry<Waypoint> entry : waypoints.entries()) {
                sendRemoveWaypointToClient(playerId, entry.key());
            }
            waypoints.clear();
        }
        playerWaypoints.remove(playerId);
    }

    public static void removePlayerData(ServerPlayer player) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        playerWaypoints.remove(playerId);
    }

    private static int findNextFreeColor(IntObjectMap<Waypoint> waypoints) {
        for (int i = 0; i < MAX_COLORS; i++) {
            if (!waypoints.containsKey(i)) {
                return i;
            }
        }
        return 0;
    }

    private static void sendWaypointToClient(String playerId, String levelId, BlockPos targetPos, int colorId) {
        Player p = PlayerUtil.getPlayer(playerId, PlayerUtil.PlayerNameSpace.SERVER);
        if (p == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("levelId", levelId);
        json.addProperty("targetPos", HBUtil.BlockUtil.positionToString(targetPos));
        json.addProperty("colorId", colorId);

        SimpleStringMessage.createAndFire(p, MSG_ID_MOVING_WAYPOINT, json.toString());
    }

    private static void sendRemoveWaypointToClient(String playerId, int colorId) {
        Player p = PlayerUtil.getPlayer(playerId, PlayerUtil.PlayerNameSpace.SERVER);
        if (p == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("colorId", colorId);

        SimpleStringMessage.createAndFire(p, MSG_ID_MOVING_WAYPOINT, json.toString());
    }
}