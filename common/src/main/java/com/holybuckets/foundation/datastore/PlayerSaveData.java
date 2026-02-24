package com.holybuckets.foundation.datastore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.holybuckets.foundation.GeneralConfig.GSON;

/**
 * Stores a levelID and any data associated with a level that we want to persist
 */
public class PlayerSaveData {
    final Map<String, JsonElement> playerNbt;

    /** STATICS **/


    /** ######### **/


    /** Constructors **/

    public PlayerSaveData() {
        super();
        this.playerNbt = new ConcurrentHashMap<>();
    }

    public PlayerSaveData(JsonObject json) {
        this();
        this.fromJson(json);
    }


    /**
     * Adds a players saved data to the playerNbt map, which will be saved to disk
     * @param player
     * @param data
     */
    public void save(Player player, CompoundTag data) {
        String playerId = HBUtil.PlayerUtil.getId(player);
        playerNbt.put(playerId, HBUtil.NetworkUtil.tagToJson(data));
    }

    public JsonObject get(Player p) {
        if(!playerNbt.containsKey(HBUtil.PlayerUtil.getId(p)))
            return new JsonObject();
        return playerNbt.get(HBUtil.PlayerUtil.getId(p)).getAsJsonObject();
    }

    public JsonObject get(String playerId) {
        if(!playerNbt.containsKey(playerId))
            return new JsonObject();
        return playerNbt.get(playerId).getAsJsonObject();
    }

    /** Serializers */

    JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        this.playerNbt.forEach((key, value) -> {
            json.add(key, value);
        });

        return json;
    }

    public void fromJson(JsonObject json)
    {
        this.playerNbt.clear();
        this.playerNbt.putAll(json.asMap());
    }


    //** STATICS

    /**
     * Validates that all of the default fields are present,
     * adds them if they are not
     * @param data
     */
    public static void validate(PlayerSaveData data, WorldSaveData worldData)
    {
        if( data == null || data.playerNbt == null ) return;
        Map<String, JsonElement> props = data.playerNbt;

    }



}
