package com.holybuckets.foundation.player;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentLinkedSet;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.client.server.IntegratedServer;
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
    static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();
    static final Map<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    public static final Map<String, ManagedPlayer> PLAYERS = new ConcurrentHashMap<>();
    static final LinkedHashSet<ServerPlayer> PENDING_PLAYERS = new LinkedHashSet<>();

    public static ManagedPlayer CLIENT_PLAYER;

    private Player player;
    private String id;
    private ServerPlayer serverPlayer;
    private long tickWritten;
    private long tickLoaded;
    private CompoundTag holdNbt;
    private final HashMap<Class<? extends IManagedPlayer>, IManagedPlayer> managedPlayerData = new HashMap<>();

    //utility
    public final ConcurrentLinkedSet<Entity> nearbyLivingEntities;


    public ManagedPlayer() {
        super();
        this.nearbyLivingEntities = new ConcurrentLinkedSet<>();
    }

    public ManagedPlayer(Player player)
    {
        this();
        this.player = player;
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
        if(player instanceof ServerPlayer sp) {
            this.serverPlayer = sp;
            PENDING_PLAYERS.add(sp);
            if(GeneralConfig.getInstance().getServer() instanceof IntegratedServer)
                CLIENT_PLAYER = this;
        }
        //player is not defined yet here, cannot collect id, but have ref to player
    }

    public ManagedPlayer(Player player, String id)
    {
        this(player);
        this.id = id;
        if(player instanceof ServerPlayer sp) {
            PLAYERS.put(this.id, this);
        }
    }

    public ManagedPlayer(CompoundTag tag) {
        this();
        this.deserializeNBT(tag);
        if(tag == null || tag.isEmpty() ) {
            LoggerBase.logDebug(null, "004000", "Not NBT data found in ManagedPlayer( CompoundTag )" + tag.toString());
            return;
        }
        if( this.id == null ) {
            LoggerBase.logDebug(null, "004001", "Failed to read playerId from NBT data " + tag);
        }

        PLAYERS.put(this.id, this);
        if(GeneralConfig.getInstance().getServer() instanceof IntegratedServer)
            CLIENT_PLAYER = this;
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
        this.player = p;
        if (p instanceof ServerPlayer) {
            this.serverPlayer = (ServerPlayer) p;
            String id = HBUtil.PlayerUtil.getId(p);
            PLAYERS.put(id, this);
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

        if (this.player != null)
        {
            if(id==null) id = HBUtil.PlayerUtil.getId(player);
            try {
                this.initSubclassesFromNbt(this.holdNbt);
                this.holdNbt = null; // Clear the held NBT after processing
                this.onPlayerJoinComplete();
            } catch (InvalidId e) {
                String msg = String.format("Invalid id: initializing ManagedPlayer from NBT for player %s: %s",
                        player.getName().getString(), e.getMessage());
                LoggerBase.logError(null, "004005", msg);
                return false;
            }
            return true;
        }
        return false;
    }

    //** CORE **//
    private static int MOB_DETECTION_RADIUS = 48;
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
        this.initSubclassesFromMemory();
        for(IManagedPlayer data : managedPlayerData.values())
        {
            try {
                data.handlePlayerJoin(player);
            } catch (Exception e) {
                String msg = String.format("Error handling player join for player %s, class: %s", player.getDisplayName(), data.getClass() );
                LoggerBase.logError(null, "004007", msg);
            }

        }
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


    private void onPlayerRespawn() {
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

    //** Utility
    public static ManagedPlayer getManagedPlayer(Player player)
    {
        if(!GeneralConfig.getInstance().isServerSide()) return CLIENT_PLAYER;
        if(player == null) return null;
        String id = HBUtil.PlayerUtil.getId(player);
        if(PLAYERS.containsKey(id)) {
            return PLAYERS.get(id);
        }
        return PLAYERS.getOrDefault(id, new ManagedPlayer(player));
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
            IManagedPlayer sub = data.getValue().get();
            if( sub == null ) continue;
            if( managedPlayerData.containsKey(sub.getClass()) ) {
                sub = managedPlayerData.get(sub.getClass());
            }
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

        if(!errors.isEmpty()) {
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            throw new InvalidId(error.toString());
        }
    }

    //** SERIALIZERS **//

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        
        if(this.player == null) {
            //LoggerBase.logError(null, "004001", "ManagedPlayer not initialized properly");
            return tag;
        }

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
        if(tag == null || tag.isEmpty()) {
            this.holdNbt = new CompoundTag();
            return;
        }

        try {
            this.tickWritten = tag.getLong("tickWritten");
            this.id = tag.getString("id");
            this.holdNbt = tag; // Store the NBT for later use
        } catch (Exception e) {
            LoggerBase.logError(null, "004003", "Error deserializing ManagedPlayer: " + e.getMessage());
        }
        PLAYERS.put(this.getId(), this);
    }

    private void save() {
        if(this.player == null || this.player.isRemoved()) return;

        try {
            CompoundTag tag = this.serializeNBT();
            if(tag.isEmpty()) return;
            if(this.serverPlayer != null) {
                //serverPlayer.addAdditionalSaveData(tag); //Does not add my data to the player, just saves other serverLevelData
            }
        } catch (Exception e) {
            LoggerBase.logError(null, "004004", "Error saving ManagedPlayer: " + e.getMessage());
        }
    }

    public static void registerManagedPlayerData(Class<? extends IManagedPlayer> classObject, Supplier<IManagedPlayer> data) {
        MANAGED_SUBCLASSES.put(classObject, data);
    }


    //** EVENT
    public static void onClientConnectedToServer(Player player) {
        if(CLIENT_PLAYER == null)
            CLIENT_PLAYER = new ManagedPlayer(player, HBUtil.PlayerUtil.getId(player));
        CLIENT_PLAYER.initJoinedPlayer(player);
    }

    //HERE
    public static void onPlayerLogin(PlayerLoginEvent event)
    {

        Player player = event.getPlayer();
        if(player instanceof  ServerPlayer sp)
        {
            String id = HBUtil.PlayerUtil.getId(player);
            ManagedPlayer mp = PLAYERS.get(id);
            if(mp == null) mp = new ManagedPlayer(player, id);

            PLAYERS.put(id , mp);
            PENDING_PLAYERS.add(sp);
            mp.initJoinedPlayer(player);
        }
    }

    public static void onPlayerLogout(PlayerLogoutEvent event) {
        Player player = event.getPlayer();
        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            //mp.save();
            mp.onPlayerLeave();
        }
    }

    private static void onPlayerRespawn(PlayerRespawnEvent event)
    {
        PENDING_PLAYERS.remove(event.getOldPlayer());
        ManagedPlayer mp = PLAYERS.get( HBUtil.PlayerUtil.getId(event.getOldPlayer()) );
        if(mp == null) {
            LoggerBase.logError(null, "004008", "ManagedPlayer not found for respawn event");
            return;
        }
        Player p = event.getNewPlayer();
        mp.setPlayer(p);
        mp.onPlayerRespawn();
    }

    private static void onPlayerDeath(LivingDeathEvent event)
    {
        if(!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.handlePlayerDeath(player);
        } else {
            LoggerBase.logError(null, "004010", "ManagedPlayer not found for death event");
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

    private static void onDigSpeed(DigSpeedEvent event) {
        Player player = event.getPlayer();
        if(player == null) return;

        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp != null) {
            mp.handlePlayerDigSpeed(player, event.getSpeed(), event.getSpeedOverride());
        } else {
            //LoggerBase.logError(null, "004014", "ManagedPlayer not found for dig speed event");
        }
    }

    private static void onPlayerAttack(PlayerAttackEvent playerAttackEvent) {
        Player player = playerAttackEvent.getPlayer();
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


    public static void onServerTick(ServerTickEvent e)
    {
        if(PENDING_PLAYERS.isEmpty()) return;

        Iterator<ServerPlayer> mp = PENDING_PLAYERS.iterator();
        while(mp.hasNext())
        {
            Player p = mp.next();
            if(p == null) {
                mp.remove(); continue;
            }
            ManagedPlayer pending = PLAYERS.get(HBUtil.PlayerUtil.getId(p));
            if( pending == null) continue;
            if( pending.initJoinedPlayer(p) ) mp.remove(); // Remove after processing
        }

    }

    public static void on20ServerTicks(ServerTickEvent e) {
        for(ManagedPlayer mp : PLAYERS.values()) {
            mp.updateNearbyMobs();
        }
    }

    public static void onServerStarting(ServerStartingEvent event) {
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
        CLIENT_PLAYER = null;
    }


    public static void onServerStarted(ServerStartedEvent event) {
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
        HBUtil.PlayerUtil.getAllPlayers().forEach(PENDING_PLAYERS::add);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (ManagedPlayer player : PLAYERS.values()) {
            //player.save();
        }
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
    }


    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerAttack(ManagedPlayer::onPlayerAttack, EventPriority.High);
        reg.registerOnDigSpeedEvent(ManagedPlayer::onDigSpeed, EventPriority.High);
        reg.registerOnPlayerDeath(ManagedPlayer::onPlayerDeath, EventPriority.Highest);
        reg.registerOnPlayerRespawn(ManagedPlayer::onPlayerRespawn, EventPriority.Highest);
        reg.registerOnPlayerLogin(ManagedPlayer::onPlayerLogin, EventPriority.High);
        reg.registerOnPlayerLogout(ManagedPlayer::onPlayerLogout, EventPriority.Lowest);

        reg.registerOnBeforeServerStarted(ManagedPlayer::onServerStarting, EventPriority.Highest);
        //reg.registerOnServerStarted(ManagedPlayer::onServerStarted, EventPriority.Highest);
        reg.registerOnServerStopped(ManagedPlayer::onServerStopped, EventPriority.Lowest);
        reg.registerOnServerTick(TickType.ON_SINGLE_TICK, ManagedPlayer::onServerTick, EventPriority.Lowest);
        reg.registerOnServerTick(TickType.ON_20_TICKS, ManagedPlayer::on20ServerTicks, EventPriority.Highest);
    }



}
