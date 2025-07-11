package com.holybuckets.foundation.sample;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.modelInterface.IManagedPlayer;
import net.blay09.mods.balm.api.event.BreakBlockEvent;
import net.blay09.mods.balm.api.event.PlayerAttackEvent;
import net.blay09.mods.balm.api.event.PlayerChangedDimensionEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

import static com.holybuckets.foundation.player.ManagedPlayer.registerManagedPlayerData;

public class SamplePlayerData implements IManagedPlayer {

    int blocksBroken;
    int damageDealt;
    private String id;
    Player p;

    static {
        //registerManagedPlayerData(SamplePlayerData.class, () -> new SamplePlayerData(null));
    }


    public SamplePlayerData(Player p) {
        blocksBroken = 0;
        damageDealt = 0;
        this.p = p;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public boolean isClientOnly() {
        return false;
    }

    @Override
    public boolean isInit(String subclass) {
        return false;
    }

    @Override
    public IManagedPlayer getStaticInstance(Player player, String id) {
        if(!(player instanceof ServerPlayer)) {
            return null;
        }
        return playerDataMap.get(player);
    }

    @Override
    public void handlePlayerJoin(Player player) {

    }

    @Override
    public void handlePlayerLeave(Player player) {
        if( player != null)
        playerDataMap.remove(player);
    }

    @Override
    public void handlePlayerRespawn(Player player) {
        if( player != null)
            playerDataMap.remove(player);
        playerDataMap.put(player, this);
    }

    @Override
    public void handlePlayerDeath(Player player) {

    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("blocksBroken", blocksBroken);
        nbt.putInt("damageDealt", damageDealt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        blocksBroken = nbt.getInt("blocksBroken");
        damageDealt = nbt.getInt("damageDealt");
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setPlayer(Player player) {
        if(player != null) {
            this.p = player;
            playerDataMap.putIfAbsent(player , this);
        }
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerAttack(SamplePlayerData::onPlayerAttack);
        reg.registerOnBreakBlock(SamplePlayerData::onPlayerBreakBlock);
        reg.registerOnPlayerChangedDimension(SamplePlayerData::onPlayerChangedDimension);
    }


    //** EVENTS
    static Map<Player, SamplePlayerData> playerDataMap = new HashMap<>();
    public static void onPlayerAttack(PlayerAttackEvent event) {
        Player p = event.getPlayer();
        if(playerDataMap.containsKey(p)) {
            SamplePlayerData data = playerDataMap.get(p);
            data.damageDealt++;
        }
    }

    public static void onPlayerBreakBlock(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if(p != null && playerDataMap.containsKey(p)) {
            SamplePlayerData data = playerDataMap.get(p);
            data.blocksBroken++;
        }
    }

    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        LoggerBase.logInfo(null,
        "SamplePlayerData",
         "Player " + event.getPlayer().getName().getString() + " changed dimension to " + event.getToDim());
    }



}
