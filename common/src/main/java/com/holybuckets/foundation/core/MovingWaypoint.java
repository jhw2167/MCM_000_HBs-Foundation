package com.holybuckets.foundation.core;

import com.google.gson.JsonObject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.EntityLike;
import com.holybuckets.foundation.model.EntityLikeResolver;
import com.holybuckets.foundation.model.VanillaEntityLike;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.player.ManagedPlayer;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.holybuckets.foundation.HBUtil.PlayerUtil;

public class MovingWaypoint {

    public static class Waypoint {
        String levelId;
        BlockPos targetPos;
        int colorId;
        // --- extended state ---
        int waypointId;          // unique id; defaults to colorId for simple waypoints
        boolean isPermanent;     // if true, the client should NOT auto-clear via dwell-near
        // Persistable identifier for the linked entity. We hold the UUID rather than the
        // Entity itself so we don't pin unloaded entities in memory and so the value
        // survives save/load (NBT) and reconnect cycles. Look the entity up on demand
        // via ServerLevel#getEntity(UUID) when we need its current position.
        UUID linkedEntityUuid;
        String nameTag;          // optional label, null if unset

        // Backwards-compatible constructor (existing callers continue to work).
        public Waypoint(String levelId, BlockPos targetPos, int colorId) {
            this(levelId, targetPos, colorId, colorId, false, (UUID) null, null);
        }

        // Full constructor capturing all extended fields, taking a raw UUID.
        public Waypoint(String levelId, BlockPos targetPos, int colorId, int waypointId,
                        boolean isPermanent, UUID linkedEntityUuid, String nameTag) {
            this.levelId = levelId;
            this.targetPos = targetPos;
            this.colorId = colorId % MAX_COLORS;
            this.waypointId = waypointId;
            this.isPermanent = isPermanent;
            this.linkedEntityUuid = linkedEntityUuid;
            this.nameTag = nameTag;
        }

        // Convenience constructor that captures the UUID off a live Entity.
        public Waypoint(String levelId, BlockPos targetPos, int colorId, int waypointId,
                        boolean isPermanent, EntityLike linkedEntity, String nameTag) {
            this(levelId, targetPos, colorId, waypointId, isPermanent,
                linkedEntity == null ? null : linkedEntity.getUUID(), nameTag);
        }
    }

    public static final String MSG_ID_MOVING_WAYPOINT = "moving_waypoint";
    public static final int MAX_COLORS = 16;

    //Read only waypoint info snapshot for API use
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


    private static final Map<String, IntObjectMap<Waypoint>> playerWaypoints = new HashMap<>();

    public static void setWaypoint(ServerPlayer player, BlockPos target) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());
        int nextColorId = findNextFreeColor(waypoints);
        setWaypoint(player, target, nextColorId);
    }

    public static void setWaypoint(ServerPlayer player, BlockPos target, int colorId) {
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
                                   boolean isPermanent, EntityLike linkedEntity, String nameTag) {
        String playerId = PlayerUtil.getId(player);
        if (playerId == null) return;
        String levelId = player.level().dimension().location().toString();

        IntObjectMap<Waypoint> waypoints = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());

        waypoints.put(waypointId,
            new Waypoint(levelId, target, colorId, waypointId, isPermanent, linkedEntity, nameTag));

        sendWaypointToClient(playerId, levelId, target, colorId, waypointId, isPermanent,
            (linkedEntity==null) ? null : linkedEntity.getUUID(), nameTag);
    }

    public static void removeWaypoint(ServerPlayer player, int waypointId) {
        removeWaypoint(PlayerUtil.getId(player), waypointId);
    }


    public static void removeWaypoint(String playerId, int waypointId) {
        if (playerId == null) return;
        IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints != null) {
            waypoints.remove(waypointId);
            sendRemoveWaypointToClient(playerId, waypointId);
        }
    }

    /**
     * @return all palyer active waypoints
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
     * Remove a waypoint by its waypointId
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
                                             int waypointId, boolean isPermanent, UUID linkedEntityUuid, String nameTag) {
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
        if (linkedEntityUuid != null)      json.addProperty("linkedEntityUuid", linkedEntityUuid.toString());
        if (nameTag != null && !nameTag.isEmpty()) json.addProperty("nameTag", nameTag);

        SimpleStringMessage.createAndFire(p, MSG_ID_MOVING_WAYPOINT, json.toString());
    }

    private static void sendRemoveWaypointToClient(String playerId, int waypointId) {
        Player p = PlayerUtil.getPlayer(playerId, PlayerUtil.PlayerNameSpace.SERVER);
        if (p == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("waypointId", waypointId);

        SimpleStringMessage.createAndFire(p, MSG_ID_MOVING_WAYPOINT, json.toString());
    }

    //** LIFECYCLE & ENTITY RESYNC

    public static void init(EventRegistrar reg) {
        EntityLikeResolver.register((uuid, level) -> {
            if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
            Entity entity = serverLevel.getEntity(uuid);
            return entity == null ? Optional.empty() : Optional.of(new VanillaEntityLike(entity));
        });
        reg.registerOnServerTick(TickType.ON_20_TICKS, MovingWaypoint::onEntityResyncTick);
        PlayerWaypointData.init();
    }

    private static void onEntityResyncTick(ServerTickEvent event)
    {
        if (playerWaypoints.isEmpty() || ManagedPlayer.PLAYERS.isEmpty()) return;

        for (Map.Entry<String, IntObjectMap<Waypoint>> playerEntry : playerWaypoints.entrySet()) {
            String playerId = playerEntry.getKey();
            Player p = PlayerUtil.getPlayer(playerId, PlayerUtil.PlayerNameSpace.SERVER);
            if (!(p instanceof ServerPlayer sp)) continue;

            for (IntObjectMap.PrimitiveEntry<Waypoint> e : playerEntry.getValue().entries()) {
                Waypoint w = e.value();
                if (w.linkedEntityUuid == null) continue;

                // Resolve the linked UUID to an EntityLike with resolver to handle non minecraft entities
                Optional<EntityLike> resolved = EntityLikeResolver.resolveEntity(w.linkedEntityUuid, sp.serverLevel());
                if (resolved.isEmpty() || !resolved.get().isValid()) continue;

                BlockPos newPos = resolved.get().blockPosition();
                if (!newPos.equals(w.targetPos)) {
                    w.targetPos = newPos;
                    sendWaypointToClient(playerId, w.levelId, w.targetPos, w.colorId,
                        w.waypointId, w.isPermanent, w.linkedEntityUuid, w.nameTag);
                }
            }
        }
    }

    //** PERSISTENCE — IManagedPlayer
    public static class PlayerWaypointData implements IManagedPlayer {

        private String id;
        private Player p;

        public PlayerWaypointData(Player player) {
            setPlayer(player);
        }

        //STOPPING DEFAULT WAYPOINT SAVING
        public static void init() {
            ManagedPlayer.registerManagedPlayerData(PlayerWaypointData.class, () -> new PlayerWaypointData(null));
        }

        @Override public boolean isServerOnly() { return true; }
        @Override public boolean isInit(String subclass) { return true; }
        @Override public IManagedPlayer getStaticInstance(Player player, String id) { return null; }

        @Override
        public void handlePlayerJoin(Player player) {
            // After deserializeNBT has populated playerWaypoints, push everything back to
            // the client so the user sees their persisted waypoints on login.
            if (!(player instanceof ServerPlayer sp)) return;
            String playerId = PlayerUtil.getId(sp);
            if (playerId == null) return;
            IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
            if (waypoints == null || waypoints.isEmpty()) return;

            for (IntObjectMap.PrimitiveEntry<Waypoint> e : waypoints.entries()) {
                Waypoint w = e.value();
                sendWaypointToClient(playerId, w.levelId, w.targetPos, w.colorId,
                    w.waypointId, w.isPermanent, w.linkedEntityUuid, w.nameTag);
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (p == null) return tag;

            String playerId = PlayerUtil.getId(p);
            if (playerId == null) return tag;
            IntObjectMap<Waypoint> waypoints = playerWaypoints.get(playerId);
            if (waypoints == null || waypoints.isEmpty()) return tag;

            ListTag list = new ListTag();
            for (IntObjectMap.PrimitiveEntry<Waypoint> e : waypoints.entries()) {
                Waypoint w = e.value();
                CompoundTag c = new CompoundTag();
                c.putString("levelId", w.levelId == null ? "" : w.levelId);
                c.putString("targetPos", HBUtil.BlockUtil.positionToString(w.targetPos));
                c.putInt("colorId", w.colorId);
                c.putInt("waypointId", w.waypointId);
                if (w.isPermanent)               c.putBoolean("isPermanent", true);
                if (w.linkedEntityUuid != null)  c.putUUID("linkedEntityUuid", w.linkedEntityUuid);
                if (w.nameTag != null)           c.putString("nameTag", w.nameTag);
                list.add(c);
            }
            tag.put("waypoints", list);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (p == null) return;
            String playerId = PlayerUtil.getId(p);
            if (playerId == null) return;

            if (!nbt.contains("waypoints", Tag.TAG_LIST)) return;
            ListTag list = nbt.getList("waypoints", Tag.TAG_COMPOUND);
            if (list.isEmpty()) return;

            IntObjectMap<Waypoint> map = playerWaypoints.computeIfAbsent(playerId, k -> new IntObjectHashMap<>());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                BlockPos targetPos = new BlockPos(HBUtil.BlockUtil.stringToBlockPos(c.getString("targetPos")));
                int colorId = c.getInt("colorId");
                int waypointId = c.contains("waypointId") ? c.getInt("waypointId") : colorId;
                boolean isPermanent = c.getBoolean("isPermanent");
                UUID linkedEntityUuid = c.hasUUID("linkedEntityUuid") ? c.getUUID("linkedEntityUuid") : null;
                String nameTag = c.contains("nameTag") ? c.getString("nameTag") : null;
                String levelId = c.contains("levelId") ? c.getString("levelId") : "";

                map.put(colorId, new Waypoint(levelId, targetPos, colorId, waypointId,
                    isPermanent, linkedEntityUuid, nameTag));
            }
        }

        @Override public void setId(String id) { this.id = id; }

        @Override
        public void setPlayer(Player player) {
            if (player != null) this.p = player;
        }
    }
}