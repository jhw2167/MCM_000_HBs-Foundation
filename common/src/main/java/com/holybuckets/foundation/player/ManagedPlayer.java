package com.holybuckets.foundation.player;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedPlayer {
    public static final String CLASS_ID = "004";
    static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();
    static final Map<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    public static final Map<String, ManagedPlayer> PLAYERS = new ConcurrentHashMap<>();
    static final List<ManagedPlayer> PENDING_PLAYERS = new ArrayList<>();

    private Player player;
    private String id;
    private ServerPlayer serverPlayer;
    private long tickWritten;
    private long tickLoaded;
    private final HashMap<Class<? extends IManagedPlayer>, IManagedPlayer> managedPlayerData = new HashMap<>();

    public ManagedPlayer() {
        super();
    }

    public ManagedPlayer(Player player)
    {
        this();
        this.player = player;
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
        if(player instanceof ServerPlayer) {
            this.serverPlayer = (ServerPlayer) player;
        }
        PENDING_PLAYERS.add(this);
    }

    public ManagedPlayer(Player player, String id)
    {
        this();
        this.player = player;
        this.id = id;
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
        if(player instanceof ServerPlayer) {
            this.serverPlayer = (ServerPlayer) player;
        }
    }

    public ManagedPlayer(CompoundTag tag) {
        this();
        this.deserializeNBT(tag);
        PENDING_PLAYERS.add(this);
        PLAYERS.put(this.id, this);
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
        if (classObject == null || data == null) {
            return false;
        }
        managedPlayerData.put(classObject, data);
        return true;
    }

    private void onPlayerJoin(Player player) {
        this.initSubclassesFromMemory();
        for(IManagedPlayer data : managedPlayerData.values()) {
            data.handlePlayerJoin(player);
        }
    }

    private void onPlayerLeave() {
        for(IManagedPlayer data : managedPlayerData.values()) {
            data.handlePlayerLeave(player);
        }
    }

    //** Utility
    public static ManagedPlayer getManagedPlayer(Player player) {
        String id = HBUtil.PlayerUtil.getId(player);
        return PLAYERS.getOrDefault(id, new ManagedPlayer(player));
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

            if( sub.getStaticInstance(player, playerId) != null ) {
                sub = sub.getStaticInstance(player, playerId);
            }
            sub.setPlayer(player);
            this.setSubclass(sub.getClass(), sub);

        }
    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId
    {
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

            try {
                CompoundTag nbt = tag.getCompound(sub.getClass().getName());
                if(managedPlayerData.containsKey(sub.getClass())) {
                    managedPlayerData.get(sub.getClass()).deserializeNBT(nbt);
                } else {
                    sub.setPlayer(this.player);
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

    //HERE
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        
        if(this.player == null) {
            LoggerBase.logError(null, "004001", "ManagedPlayer not initialized properly");
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

    public void deserializeNBT(CompoundTag tag) {
        if(tag == null || tag.isEmpty()) return;

        try {
            this.tickWritten = tag.getLong("tickWritten");
            this.id = tag.getString("id");
            initSubclassesFromNbt(tag);
        } catch (InvalidId e) {
            LoggerBase.logError(null, "004003", "Error deserializing ManagedPlayer: " + e.getMessage());
        }
    }

    private void save() {
        if(this.player == null || this.player.isRemoved()) return;

        try {
            CompoundTag tag = this.serializeNBT();
            if(tag.isEmpty()) return;
            if(this.serverPlayer != null) {
                serverPlayer.addAdditionalSaveData(tag);
            }
        } catch (Exception e) {
            LoggerBase.logError(null, "004004", "Error saving ManagedPlayer: " + e.getMessage());
        }
    }

    public static void registerManagedPlayerData(Class<? extends IManagedPlayer> classObject, Supplier<IManagedPlayer> data) {
        MANAGED_SUBCLASSES.put(classObject, data);
    }


    //** EVENT

    //HERE
    public static void onPlayerLogin(PlayerLoginEvent event)
    {
        Player player = event.getPlayer();
        String id = HBUtil.PlayerUtil.getId(player);
        ManagedPlayer mp = PLAYERS.get(id);
        if(mp == null) {
            for(ManagedPlayer pending : PENDING_PLAYERS) {
                if(pending.getId().equals(id) || pending.player.equals(player)) {
                    mp = pending;
                    PENDING_PLAYERS.remove(pending);
                    break;
                }
            }
            if(mp == null) mp = new ManagedPlayer(player, id);
        }
        PLAYERS.put(id , mp);
        mp.onPlayerJoin(player);
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

    public static void onServerStopped(ServerStoppedEvent event) {
        for (ManagedPlayer player : PLAYERS.values()) {
            player.save();
        }
        PLAYERS.clear();
        PENDING_PLAYERS.clear();
    }



    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerLogin(ManagedPlayer::onPlayerLogin, EventPriority.High);
        reg.registerOnPlayerLogout(ManagedPlayer::onPlayerLogout, EventPriority.Lowest);
        reg.registerOnServerStopped(ManagedPlayer::onServerStopped, EventPriority.Lowest);
    }
}
