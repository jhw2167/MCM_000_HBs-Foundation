package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.structure.StructureInfo;
import com.holybuckets.foundation.structure.StructureManager;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Description: StructureInfoMessage
 * A message type for sending structure information data
 */
public class StructureInfoMessage {

    public static final String LOCATION = "structure_info";
    public static final int MAX_STRUCTURES = 16; // Limit to prevent packet size issues
    
    public final UUID senderId;
    public final List<StructureInfo> structures;

    StructureInfoMessage(UUID senderId, List<StructureInfo> structures) {
        this.senderId = senderId;
        // Limit structures if it exceeds max size
        this.structures = structures != null && structures.size() > MAX_STRUCTURES 
            ? structures.subList(0, MAX_STRUCTURES) 
            : (structures != null ? structures : List.of());
    }

    public static void createAndFire(Player p, List<StructureInfo> structures) {
        HBUtil.NetworkUtil.serverSendToPlayer(p, new StructureInfoMessage(p.getUUID(), structures));
    }

    public static class StructureInfoMessageHandler {

        public static String CLASS_ID = "017";

        public static void handle(Player player, StructureInfoMessage message) {
            StructureManager.handleStructureInfoFromServer(player, message);
        }
    }
}
