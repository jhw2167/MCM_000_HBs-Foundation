package com.holybuckets.foundation.core;

import com.google.gson.JsonObject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.holybuckets.foundation.HBUtil.PlayerUtil;

public class MovingWaypoint {

    public static class Waypoint {
        String levelId;
        BlockPos targetPos;
        int colorId;
        // --- new (optional) state ---
        int waypointId;       // unique id; defaults to colorId for simple waypoints
        boolean isPermanent;  // if true, the client should NOT auto-clear via dwell-near
        Entity linkedEntity;  // server-side entity reference for "moves with entity" waypoints; null if none
        String nameTag;       // optional label, null if unset

        // Backwards-compatible constructor (existing callers continue to work).
        public Waypoint(String levelId, BlockPos targetPos, int colorId) {
            this(levelId, targetPos, colorId, colorId, false, null, null);
        }

        // Full constructor capturing all new fields.
        public Waypoint(String levelId, BlockPos targetPos, int colorId, int waypointId,
                        boolean isPermanent, Entity linkedEntity, String nameTag) {
            this.levelId = levelId;
            this.targetPos = targetPos;
            this.colorId = colorId;
            this.waypointId = waypointId;
            this.isPermanent = isPermanent;
            this.linkedEntity = linkedEntity;
            this.nameTag = nameTag;
        }
    }

    public static final String MSG_ID_MOVING_WAYPOINT = "moving_waypoint";
    // Wool-color range. Public so callers (e.g. WaypointStick) can seed their own
    // id counters relative to it.
    public static final int MAX_COLORS = 16;

    /**
     * Read-only snapshot of a single waypoint for callers that want to inspect state
     * (e.g. find the nearest waypoint to remove). The inner {@link Waypoint} stays
     * private so its mutability is not part of the public API.
     */
    public static final class WaypointInfo {
        public final int colorId;
        public final int waypointId;
        public final BlockPos targetPos;
        public final boolean isPermanent;
        public final String nameTag;

        WaypointInfo(int colorId, int waypointId, BlockPos targetPos, boolean isPermanent, String nameTag) {
            this.colorId = colorId;
            this.waypointId = waypointId;
            this.targetPos = targetPos;
            this.isPermanent = isPermanent;
            this.nameTag = nameTag;
        }
    }

    private static WaypointInfo toInfo(Waypoint w) {
        return new WaypointInfo(w.colorId, w.waypointId, w.targetPos, w.isPermanent, w.nameTag);
    }

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
        // Backwards-compatible path: simple waypoint, waypointId == colorId, not permanent,
        // no linked entity, no name tag.
        setWaypoint(player, target, colorId, colorId, false, null, null);
    }

    /**
     * Extended waypoint API.
     *
     * @param player         the firing player (server-side)
     * @param target         world block position the waypoint points at
     * @param colorId        wool color id (rendering)
     * @param waypointId     unique id for this waypoint slot; pass {@code colorId} for simple waypoints
     * @param isPermanent    if true, the client will skip dwell-near auto-deletion for this waypoint
     * @param linkedEntity   optional entity the waypoint should track (server holds the ref;
     *                       its UUID is what crosses the wire), may be {@code null}
     * @param nameTag        optional label to render alongside the waypoint, may be {@code null}
     */
    public static void setWaypoint(ServerPlayer player, BlockPos target, int colorId, int waypointId,
                                   boolean isPermanent, Entity linkedEntity, String nameTag) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        String levelId = player.level().dimension().location().toString();

        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());

        waypoints.put(colorId,
            new Waypoint(levelId, target, colorId, waypointId, isPermanent, linkedEntity, nameTag));

        sendWaypointToClient(playerId, levelId, target, colorId, waypointId, isPermanent, linkedEntity, nameTag);
    }

    public static void removeWaypoint(ServerPlayer player, int colorId) {
        removeWaypoint(PlayerUtil.getId(player), colorId);
    }


    public static void removeWaypoint(String playerId, int colorId) {
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints != null) {
            waypoints.remove(colorId);
            sendRemoveWaypointToClient(playerId, colorId);
        }
    }

    /**
     * @return all waypoints currently registered for the given player. Empty
     * collection if the player has none (never {@code null}). The returned views
     * are snapshots; mutating them does not affect tracked state.
     */
    public static Collection<WaypointInfo> getAllWaypoints(ServerPlayer player) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return Collections.emptyList();
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints == null || waypoints.isEmpty()) return Collections.emptyList();
        List<WaypointInfo> result = new ArrayList<>(waypoints.size());
        for (IntObjectMap.PrimitiveEntry<Waypoint> e : waypoints.entries()) {
            result.add(toInfo(e.value()));
        }
        return result;
    }

    /**
     * @return the waypoint nearest to {@code position} within {@code maxHorizDist}
     * blocks (xz-only distance), or {@code null} if none qualify.
     */
    public static WaypointInfo getNearestWaypoint(ServerPlayer player, Vec3 position, double maxHorizDist) {
        if (player == null || position == null) return null;
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return null;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints == null || waypoints.isEmpty()) return null;

        Waypoint best = null;
        double bestDistSq = maxHorizDist * maxHorizDist;
        for (IntObjectMap.PrimitiveEntry<Waypoint> e : waypoints.entries()) {
            Waypoint w = e.value();
            double dx = (w.targetPos.getX() + 0.5) - position.x;
            double dz = (w.targetPos.getZ() + 0.5) - position.z;
            double distSq = dx * dx + dz * dz;
            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                best = w;
            }
        }
        return best == null ? null : toInfo(best);
    }

    /**
     * Remove a waypoint by its {@code waypointId}. The internal map is still keyed
     * by {@code colorId}, so this scans for a matching record. If you already know
     * the colorId (i.e. simple waypoints where {@code waypointId == colorId}), prefer
     * {@link #removeWaypoint(ServerPlayer, int)}.
     */
    public static void removeWaypointById(ServerPlayer player, int waypointId) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints == null) return;

        int matchKey = Integer.MIN_VALUE;
        for (IntObjectMap.PrimitiveEntry<Waypoint> e : waypoints.entries()) {
            if (e.value().waypointId == waypointId) {
                matchKey = e.key();
                break;
            }
        }
        if (matchKey != Integer.MIN_VALUE) {
            removeWaypoint(playerId, matchKey);
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
        // Simple waypoint delegate: waypointId defaults to colorId, all extended fields off.
        sendWaypointToClient(playerId, levelId, targetPos, colorId, colorId, false, null, null);
    }

    private static void sendWaypointToClient(String playerId, String levelId, BlockPos targetPos, int colorId,
                                             int waypointId, boolean isPermanent, Entity linkedEntity, String nameTag) {
        Player p = PlayerUtil.getPlayer(playerId, PlayerUtil.PlayerNameSpace.SERVER);
        if (p == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("levelId", levelId);
        json.addProperty("targetPos", HBUtil.BlockUtil.positionToString(targetPos));
        json.addProperty("colorId", colorId);
        // Extended fields: only emit when they differ from defaults so old clients ignoring
        // unknown fields remain compatible and the JSON stays small.
        if (waypointId != colorId)         json.addProperty("waypointId", waypointId);
        if (isPermanent)                   json.addProperty("isPermanent", true);
        if (linkedEntity != null)          json.addProperty("linkedEntityUuid", linkedEntity.getUUID().toString());
        if (nameTag != null && !nameTag.isEmpty()) json.addProperty("nameTag", nameTag);

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