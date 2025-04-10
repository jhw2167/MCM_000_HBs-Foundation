package com.holybuckets.foundation.player;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedPlayer {
    public static final String CLASS_ID = "004";
    static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();
    static final Map<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    static final Map<String, ManagedPlayer> PLAYERS = new ConcurrentHashMap<>();

    private String id;
    private Player player;
    private ServerPlayer serverPlayer;
    private long tickWritten;
    private long tickLoaded;
    private final HashMap<Class<? extends IManagedPlayer>, IManagedPlayer> managedPlayerData = new HashMap<>();

    public ManagedPlayer(Player player) {
        this.player = player;
        this.id = player.getUUID().toString();
        this.tickLoaded = GENERAL_CONFIG.getTotalTickCount();
        initSubclassesFromMemory(player, id);

        if(player instanceof ServerPlayer) {
            this.serverPlayer = (ServerPlayer) player;
        }
    }

    public ManagedPlayer(CompoundTag tag) {
        this.deserializeNBT(tag);
    }

    public String getId() {
        return id;
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

    private void initSubclassesFromMemory(Player player, String playerId) {
        for(Map.Entry<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> data : MANAGED_SUBCLASSES.entrySet()) {
            IManagedPlayer instance = data.getValue().get();
            if( instance.getStaticInstance(player, playerId) != null ) {
                instance = instance.getStaticInstance(player, playerId);
            }
            instance.setPlayer(player);
            if( instance.isServerOnly() && (player instanceof ServerPlayer) ) {
                setSubclass(data.getKey(), instance);
            }
            if (instance.isClientOnly() && !(player instanceof ServerPlayer) ) {
                setSubclass(data.getKey(), instance);
            }

        }
    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId {
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IManagedPlayer>, Supplier<IManagedPlayer>> data : MANAGED_SUBCLASSES.entrySet()) {
            IManagedPlayer sub = data.getValue().get();
            try {
                CompoundTag nbt = tag.getCompound(sub.getClass().getName());
                sub.setId(this.id);
                sub.setPlayer(this.player);

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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        
        if(this.id == null || this.player == null) {
            LoggerBase.logError(null, "004001", "ManagedPlayer not initialized properly");
            return tag;
        }

        try {
            tag.putString("id", this.id);
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
            this.id = tag.getString("id");
            this.tickWritten = tag.getLong("tickWritten");
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

    public static void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        ManagedPlayer mp = new ManagedPlayer(player);
        PLAYERS.put(player.getUUID().toString(), mp);
        
        for(IManagedPlayer data : mp.managedPlayerData.values()) {
            data.handlePlayerJoin(player);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (ManagedPlayer player : PLAYERS.values()) {
            player.save();
        }
        PLAYERS.clear();
    }



    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerLogin(ManagedPlayer::onPlayerLogin, EventPriority.Highest);
        reg.registerOnServerStopped(ManagedPlayer::onServerStopped, EventPriority.Lowest);
    }
}
