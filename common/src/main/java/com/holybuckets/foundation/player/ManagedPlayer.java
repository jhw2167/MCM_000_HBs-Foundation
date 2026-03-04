package com.holybuckets.foundation.player;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.PlayerSaveData;
import com.holybuckets.foundation.datastructure.ConcurrentLinkedSet;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import com.holybuckets.foundation.networking.ManagedPlayerSyncMessage;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


public class ManagedPlayer {
    public static final String CLASS_ID = "004";
    public static final ManagedPlayer DEFAULT_PLAYER = new ManagedPlayer();
    static GeneralConfig GENERAL_CONFIG;
    static final Map<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    public static final Map<String, ManagedPlayer> PLAYERS = new ConcurrentHashMap<>();
    static final Map<ServerPlayer, CompoundTag> PENDING_PLAYERS = new HashMap();

    public static ManagedPlayer CLIENT_PLAYER;

    private Player player;
    private String id;
    private ServerPlayer serverPlayer;
    private long tickWritten;
    private long tickLoaded;
    private boolean saveNextTick = false;
    private CompoundTag holdNbt;
    private final HashMap<Class<? extends IManagedPlayer>, IManagedPlayer> managedPlayerData = new HashMap<>();

    //utility
    public final ConcurrentLinkedSet<Entity> nearbyLivingEntities;

    public static void addPending(Player player, @Nullable CompoundTag nbtData) {
        if(player instanceof ServerPlayer sp) {
            PENDING_PLAYERS.put(sp, nbtData);
        }
    }

    private ManagedPlayer() {
        super();
        this.nearbyLivingEntities = new ConcurrentLinkedSet<>();
    }

    public ManagedPlayer(String id)
    {
        this();
        this.id = id;
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
    }

    public ManagedPlayer(Player player) {
        this(HBUtil.PlayerUtil.getId(player));
    }


    public ManagedPlayer(CompoundTag tag)
    {
        this();
        this.holdNbt = tag;
        try {
            this.initSubclassesFromNbt(holdNbt);
        } catch (InvalidId ex) {
            LoggerBase.logError(null, "004005", "Error initializing ManagedPlayer from NBT: " + ex.getMessage());
        }

        if(tag == null || tag.isEmpty() ) {
            LoggerBase.logDebug(null, "004000", "Not NBT data found in ManagedPlayer( CompoundTag )" + tag.toString());
            return;
        }
        if( this.id == null ) {
            LoggerBase.logDebug(null, "004001", "Failed to read playerId from NBT data " + tag);
        }
        PLAYERS.put(id, this);
    }


    /**
     * Sets the current player instance associated with the player and Managed Players, this is called
     * - When client joins a remote server (LocalPlayer)
     * - When player finishes loading into their survival world (LocalPlayer then ServerPlayer)
     * - When a player respawns after death (ServerPlayer)
     * - When a player loads into a remote server world (ServerPlayer)
     * @param p
     */
    public void setPlayer(Player p)
    {

        if (p instanceof ServerPlayer)
        {
            this.player = p;
            this.serverPlayer = (ServerPlayer) p;
            String id = HBUtil.PlayerUtil.getId(p);
            PLAYERS.put(id, this);
            for(IManagedPlayer data : managedPlayerData.values()) {
                data.setPlayer(p);
            }
        } else if(!GENERAL_CONFIG.isIntegrated()) {
            this.player = p;
            for(IManagedPlayer data : managedPlayerData.values()) {
                data.setPlayer(p);
            }
        }

    }

    public String getId() {
        if( this.id == null) {
            this.id = HBUtil.PlayerUtil.getId(player);
        }
        return this.id;
    }

    public Player getPlayer() {
        return player;
    }

    public ServerPlayer getServerPlayer() {
        return serverPlayer;
    }

    public IManagedPlayer getSubclass(Class<? extends IManagedPlayer> classObject) {
        return managedPlayerData.get(classObject);
    }

    public Boolean setSubclass(Class<? extends IManagedPlayer> classObject, IManagedPlayer data) {
        if (classObject == null || data == null) return false;
        managedPlayerData.put(classObject, data);
        return true;
    }

    public Set<Entity> getNearbyLivingEntities() {
        return nearbyLivingEntities;
    }

    /**
     * Handles race condition between onPlayerJoin and deserializeNBT, ensuring we have
     * the nbt data before we trigger onPlayerJoin method for subclasses
     * @return
     */
    private boolean initJoinedPlayer(Player p)
    {
        //subclasses not init here, cannot set
        if(p instanceof ServerPlayer sp) {
            this.serverPlayer = sp;
            this.player = p;
        } else {
            this.player = p;
        }

        if(PENDING_PLAYERS.containsKey(p)) {
            this.holdNbt = PENDING_PLAYERS.remove(p);
        }

        id = HBUtil.PlayerUtil.getId(player);

        this.initSubclassesFromMemory();
        ManagedPlayer.deserialize(this, holdNbt);
        this.onPlayerJoinComplete();

        return true;
    }

    //** CORE **//
    private static int MOB_DETECTION_RADIUS = 64;
    private void updateNearbyMobs()
    {
        if (!(serverPlayer instanceof ServerPlayer)) return;
        Level level = serverPlayer.level();
        BlockPos playerPos = serverPlayer.blockPosition();

        // Calculate AABB boundaries
        int radius = MOB_DETECTION_RADIUS;
        int minX = playerPos.getX() - radius;
        int maxX = playerPos.getX() + radius;
        int minZ = playerPos.getZ() - radius;
        int maxZ = playerPos.getZ() + radius;

        // Calculate Y bounds (full height range around player)
        int yMax = Math.min(level.getMaxBuildHeight(), playerPos.getY() + radius);
        int yMin = Math.max(level.getMinBuildHeight(), playerPos.getY() - radius);
        AABB aabb = new AABB(minX, yMin, minZ, maxX, yMax, maxZ);

        // Query entities in this AABB
        List<Entity> entitiesInArea = level.getEntities((Entity) null, aabb, this::mobPredicate);
        nearbyLivingEntities.clear();
        nearbyLivingEntities.addAll(entitiesInArea);

    }

    private boolean mobPredicate(Entity entity) {
        if(entity == null || entity.isRemoved()) return false;
        if(entity == player || entity == serverPlayer) return false;
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity)) {
            return false;
        }
        //if(nearbyMobs.contains(entity)) return false;
        return true;
    }




    //** EVENT HANDLERS **//

    private void onPlayerJoinComplete()
    {
        for(IManagedPlayer data : managedPlayerData.values())
        {
            try {
                data.handlePlayerJoin(player);
            } catch (Exception e) {
                String msg = String.format("Error handling player join for player %s, class: %s", player.getDisplayName(), data.getClass() );
                LoggerBase.logError(null, "004007", msg);
            }

        }
        this.syncToClient();
    }

    private void onPlayerLeave() {
        for(IManagedPlayer data : managedPlayerData.values()) {
        try {
            data.handlePlayerLeave(player);
        } catch (Exception e) {
            String msg = String.format("Error handling player leave for player %s, class: %s", player.getDisplayName(), data.getClass() );
            LoggerBase.logError(null, "004006", msg);
        }

        }
    }


    private void handlePlayerRespawn() {
        for(IManagedPlayer data : managedPlayerData.values()) {
            try {
                data.handlePlayerRespawn(player);
            } catch (Exception e) {
                String msg = String.format("Error handling player respawn for player %s, class: %s", player.getDisplayName(), data.getClass() );
                LoggerBase.logError(null, "004009", msg);
            }
        }
    }

    private void handlePlayerDeath(Player player)
    {
        for(IManagedPlayer data : managedPlayerData.values()) {
            try {
                data.handlePlayerDeath(player);
            } catch (Exception e) {
                String msg = String.format("Error handling player death for player %s, class: %s", player.getDisplayName(), data.getClass() );
                LoggerBase.logError(null, "004011", msg);
            }
        }
    }

    public void handlePlayerAttack(Player player, Entity target) {
        for(IManagedPlayer data : managedPlayerData.values()) {
            try {
                data.handlePlayerAttack(player, target);
            } catch (Exception e) {
                String msg = String.format("Error handling player attack for player %s, class: %s", player.getDisplayName(), data.getClass() );
                LoggerBase.logError(null, "004012", msg);
            }
        }
    }

    private void handlePlayerDigSpeed(Player player, float originalSpeed, Float newSpeed) {
        for(IManagedPlayer data : managedPlayerData.values()) {
            try {
                data.handlePlayerDigSpeed(player, originalSpeed, newSpeed);
            } catch (Exception e) {
                //String msg = String.format("Error handling player dig speed for player %s, class: %s", player.getDisplayName(), data.getClass() );
                //LoggerBase.logError(null, "004014", "ManagedPlayer not found for dig speed event");
            }
        }
    }

    //** Utility
    public static String getIdFromTag(CompoundTag tag) {
        if(tag == null || tag.isEmpty()) return null;
        if(tag.contains(PARENT_TAG)) {
            tag = tag.getCompound(PARENT_TAG);
        }
        if(tag == null || tag.isEmpty() || !tag.contains("id")) return null;
        return tag.getString("id");
    }

    public static ManagedPlayer getManagedPlayer(CompoundTag tag)
    {
        if(tag == null || tag.isEmpty()) return null;
        return getManagedPlayer(getIdFromTag(tag));
    }

    public static ManagedPlayer getManagedPlayer(Player player) {
        if(!GENERAL_CONFIG.isServerSide()) return CLIENT_PLAYER;
        if(player == null) return null;
        String id = HBUtil.PlayerUtil.getId(player);
        return getManagedPlayer(id);
    }


    public static ManagedPlayer getManagedPlayer(String id) {
        if(!GENERAL_CONFIG.isServerSide()) return CLIENT_PLAYER;
        return PLAYERS.get(id);
    }

    @Nullable
    public static ManagedPlayer removeManagedPlayer(Player player)
    {
        String id = HBUtil.PlayerUtil.getId(player);
        return PLAYERS.remove(id);
    }

    private void initSubclassesFromMemory()
    {
        int i = 0;
        String playerId = this.getId();
        for(Map.Entry<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> data : MANAGED_SUBCLASSES.entrySet())
        {
            Class<? extends IManagedPlayer> key = data.getKey();
            IManagedPlayer sub;
            sub = managedPlayerData.computeIfAbsent(key, k -> data.getValue().get() );
            if( sub == null ) continue;

            if( sub.isServerOnly() && !(player instanceof ServerPlayer) ) {
                continue;
            }
            if( sub.isClientOnly() && (player instanceof ServerPlayer) ) {
                continue;
            }
            sub.setPlayer(this.player);

            if( sub.getStaticInstance(player, playerId) != null ) {
                sub = sub.getStaticInstance(player, playerId);
            }
            this.setSubclass(sub.getClass(), sub);

        }
    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId
    {
        if(tag == null) return;
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> data : MANAGED_SUBCLASSES.entrySet())
        {
            IManagedPlayer sub = data.getValue().get();
            if( sub == null ) continue;
            if( sub.isServerOnly() && !(this.player instanceof ServerPlayer) ) {
                continue;
            }
            if( sub.isClientOnly() && (this.player instanceof ServerPlayer) ) {
                continue;
            }
            sub.setPlayer(this.player);

            try {
                CompoundTag nbt = tag.getCompound(sub.getClass().getName());
                if(managedPlayerData.containsKey(sub.getClass())) {
                    managedPlayerData.get(sub.getClass()).deserializeNBT(nbt);
                } else {
                    sub.deserializeNBT(nbt);
                    setSubclass(sub.getClass(), sub);
                }
            } catch (Exception e) {
                errors.put(sub.getClass().getName(), e.getMessage());
            }
        }

        this.syncToClient();

        if(!errors.isEmpty()) {
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            throw new InvalidId(error.toString());
        }
    }


    private void saveToDataStore()
    {
        if(this.player == null || this.player.isRemoved()) return;
        if(!GENERAL_CONFIG.isServerSide()) return;

        try {
            CompoundTag tag = this.serializeNBT();
            if(tag.isEmpty()) return;
            PlayerSaveData playerSaveData = GENERAL_CONFIG.getPlayerSaveData();
            playerSaveData.save(this.player, tag);
            
        } catch (Exception e) {
            LoggerBase.logError(null, "004004", "Error saving ManagedPlayer to DataStore: " + e.getMessage());
        }
    }

    /**
     * Saves data to dataStore and syncs it with the client
     */
     public static void save(Player p) {
        ManagedPlayer mp = getManagedPlayer(p);
        if(mp != null) mp.saveNextTick=true;
     }

    private void save() {
        this.saveToDataStore();
        this.syncToClient();
        this.saveNextTick = false;
    }

    private void syncToClient() {
        if (serverPlayer == null) return;
        if(GENERAL_CONFIG.isClientSide()) return;
        CompoundTag tag = this.serializeNBT();
        if (tag.isEmpty()) return;
        ManagedPlayerSyncMessage msg = new ManagedPlayerSyncMessage(tag);
        HBUtil.NetworkUtil.serverSendToPlayer(serverPlayer, msg);
    }

    public void syncClient(CompoundTag tag) {
        if(tag == null || tag.isEmpty()) return;
        try {
           ManagedPlayer.deserialize(CLIENT_PLAYER, tag);
        } catch (Exception e) {
            LoggerBase.logError(null, "004016", "Error syncing ManagedPlayer from server: " + e.getMessage());
        }
    }

    public static void registerManagedPlayerData(Class<? extends IManagedPlayer> classObject, Supplier<IManagedPlayer> data) {
        MANAGED_SUBCLASSES.put(classObject, data);
    }


    //** EVENT
    public static void onClientConnectedToServer(Player player) {
        String id = HBUtil.PlayerUtil.getId(player);    //SERVER:Dev if integrated
        if(PLAYERS.containsKey(id)) {
            CLIENT_PLAYER = PLAYERS.get(id);
            return;
        }
        CLIENT_PLAYER = new ManagedPlayer(HBUtil.PlayerUtil.getId(player));
        CLIENT_PLAYER.initJoinedPlayer(player);
    }

    //HERE
    public static void onPlayerLogin(PlayerLoginEvent event)
    {
        Player player = event.getPlayer();
        if(player.getGameProfile() == null) return;
        String id = HBUtil.PlayerUtil.getId(player);

        if(player instanceof ServerPlayer sp)
        {
            ManagedPlayer mp = PLAYERS.computeIfAbsent(id, k -> new ManagedPlayer(player));
            PLAYERS.put(id, mp);
            mp.initJoinedPlayer(player);
        }
    }

    public static void onPlayerLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.save();
            mp.onPlayerLeave();
        }
    }

    private static void handlePlayerRespawn(PlayerRespawnEvent event)
    {
        if(!(event.getNewPlayer() instanceof Player)) return;

        Player newPlayer = event.getNewPlayer();
        if(GENERAL_CONFIG.isIntegrated() && (newPlayer instanceof ServerPlayer)) return;

        String id = HBUtil.PlayerUtil.getId(newPlayer);
        if(id == null)
            id = HBUtil.PlayerUtil.getId(event.getOldPlayer());
        ManagedPlayer mp = PLAYERS.get( id );

        mp.setPlayer(newPlayer);
        mp.handlePlayerRespawn();
    }

    private static void onPlayerDeath(LivingDeathEvent event)
    {
        if(!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if(GENERAL_CONFIG.isIntegrated() && player.isLocalPlayer()) return;

        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.handlePlayerDeath(player);
        } else {
            LoggerBase.logError(null, "004010", "ManagedPlayer not found for death event");
        }
    }

    private static void onDigSpeed(DigSpeedEvent event)
    {
        Player player = event.getPlayer();
        if(player == null) return;
        if(!(player instanceof ServerPlayer)) return;

        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.handlePlayerDigSpeed(player, event.getSpeed(), event.getSpeedOverride());
        } else {
            //LoggerBase.logError(null, "004014", "ManagedPlayer not found for dig speed event");
        }
    }

    private static void onPlayerAttack(PlayerAttackEvent playerAttackEvent)
    {
        Player player = playerAttackEvent.getPlayer();
        if(!(player instanceof ServerPlayer)) return;

        Entity target = playerAttackEvent.getTarget();
        if(player == null || target == null) return;

        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.handlePlayerAttack(player, target);
        } else {
            //LoggerBase.logError(null, "004013", "ManagedPlayer not found for attack event");
        }
    }

    //20Ticks, on20Ticks, onTick, serverTick
    public static void on20ServerTicks(ServerTickEvent e) {
        for(ManagedPlayer mp : PLAYERS.values()) {
            if(mp.getServerPlayer() == null) continue;
            mp.updateNearbyMobs();
            if(mp.saveNextTick)
                mp.save();
        }
    }

    /**
     * On the client we need to track when the player is changed
     * @param p
     */
    public static void onClientTick(Player p) {
        if(p == null || CLIENT_PLAYER == null) return;
        if(CLIENT_PLAYER.player == p) return;

        CLIENT_PLAYER.handlePlayerDeath(CLIENT_PLAYER.player);
        CLIENT_PLAYER.setPlayer(p);
        CLIENT_PLAYER.handlePlayerRespawn();

    }

    public static void onServerStarting(ServerStartingEvent event) {
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
        CLIENT_PLAYER = null;
    }


    public static void onServerStopped(ServerStoppedEvent event) {
        for (ManagedPlayer player : PLAYERS.values()) {
            player.save();
        }
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
    }

    public static void onDataSave(DatastoreSaveEvent ds) {
        for (ManagedPlayer player : PLAYERS.values()) {
            player.save();
        }
    }


    public static void init(EventRegistrar reg)
    {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        reg.registerOnPlayerAttack(ManagedPlayer::onPlayerAttack, EventPriority.High);
        reg.registerOnDigSpeedEvent(ManagedPlayer::onDigSpeed, EventPriority.High);
        reg.registerOnPlayerDeath(ManagedPlayer::onPlayerDeath, EventPriority.Highest);
        reg.registerOnPlayerRespawn(ManagedPlayer::handlePlayerRespawn, EventPriority.Highest);
        reg.registerOnPlayerLogin(ManagedPlayer::onPlayerLogin, EventPriority.High);
        reg.registerOnPlayerLogout(ManagedPlayer::onPlayerLogout, EventPriority.Lowest);

        reg.registerOnDataSave(ManagedPlayer::onDataSave, EventPriority.Highest);

        reg.registerOnBeforeServerStarted(ManagedPlayer::onServerStarting, EventPriority.Highest);
        //reg.registerOnServerStarted(ManagedPlayer::onServerStarted, EventPriority.Highest);
        reg.registerOnServerStopped(ManagedPlayer::onServerStopped, EventPriority.Lowest);
        //reg.registerOnServerTick(TickType.ON_SINGLE_TICK, ManagedPlayer::onServerTick, EventPriority.Lowest);
        reg.registerOnServerTick(TickType.ON_20_TICKS, ManagedPlayer::on20ServerTicks, EventPriority.Highest);
    }


    //** SERIALIZERS **//

    public static final String PARENT_TAG = "managed_player";
    public static CompoundTag serialize(ManagedPlayer mp) {
        if(mp == null) return new CompoundTag();
        CompoundTag tag = new CompoundTag();
        tag.put(PARENT_TAG, mp.serializeNBT());
        return tag;
    }

    //deserialize
    public static ManagedPlayer deserialize(ManagedPlayer mp, CompoundTag tag) {
        if(tag == null || tag.isEmpty()) return mp;
        if(tag.contains(PARENT_TAG))
            tag = tag.getCompound(PARENT_TAG);
        mp.deserializeNBT(tag);
        return mp;
    }


    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();

        try {
            if(this.getId() != null)
                tag.putString("id", this.getId());
            this.tickWritten = GENERAL_CONFIG.getTotalTickCount();
            tag.putLong("tickWritten", this.tickWritten);

            for(IManagedPlayer data : managedPlayerData.values()) {
                if(data == null) continue;
                tag.put(data.getClass().getName(), data.serializeNBT());
            }
        } catch (Exception e) {
            LoggerBase.logError(null, "004002", "Error serializing ManagedPlayer: " + e.getMessage());
        }

        return tag;
    }

    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null || tag.isEmpty()) return;
        if(this.player == null) {
            this.holdNbt = tag;
            return;
        }

        try {
            this.tickWritten = tag.getLong("tickWritten");
            this.id = tag.getString("id");

            //deserialize subclasses
            for(IManagedPlayer data : managedPlayerData.values())
            {
                try {
                    CompoundTag nbt = tag.getCompound(data.getClass().getName());
                    data.deserializeNBT(nbt);
                } catch (Exception e) {
                    String msg = String.format("Error deserializing subclass %s for player %s: %s",
                        data.getClass(), player.getDisplayName(), e.getMessage());
                    LoggerBase.logError(null, "004018", msg);
                }
            }
        } catch (Exception e) {
            LoggerBase.logError(null, "004003", "Error deserializing ManagedPlayer: " + e.getMessage());
        }
    }


}
