package com.holybuckets.foundation.player;

import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Additional data worth keeping track of for each player.
 */
public class ManagedPlayer {
    Player p;
    ServerPlayer sp;

    static Map<String, ManagedPlayer> players = new ConcurrentHashMap<>();

    public ManagedPlayer(Player p) {
        this.p = p;
        if(p instanceof ServerPlayer) {
            this.sp = (ServerPlayer) p;
        }
    }

    public String getId() {
        return p.getUUID().toString();
    }

    //** UTILITY
    public void save() {

    }





    //** EVENTS
    public static void onPlayerConnected( PlayerConnectedEvent event) {
        Player player = event.getPlayer();
        players.putIfAbsent(player.getUUID().toString(), new ManagedPlayer(player));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (Map.Entry<String, ManagedPlayer> entry : players.entrySet()) {
            ManagedPlayer player = entry.getValue();
            player.save();
        }
        players.clear();
    }

}
