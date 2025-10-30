package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.structure.StructureInfo;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Description: StructureInfoMessage
 * A message type for sending structure information data
 */
public class StructureInfoMessage {

    public static final String LOCATION = "structure_info";
    public static final int MAX_STRUCTURES = 100; // Limit to prevent packet size issues
    
    public final UUID senderId;
    public final List<StructureInfo> structures;

    public StructureInfoMessage(UUID senderId, List<StructureInfo> structures) {
        this.senderId = senderId;
        // Limit structures if it exceeds max size
        this.structures = structures != null && structures.size() > MAX_STRUCTURES 
            ? structures.subList(0, MAX_STRUCTURES) 
            : (structures != null ? structures : List.of());
    }

    public static StructureInfoMessage create(UUID senderId, List<StructureInfo> structures) {
        return new StructureInfoMessage(senderId, structures);
    }

    public static class StructureInfoMessageHandler {

        public static String CLASS_ID = "017";

        public static void handle(Player player, StructureInfoMessage message) {
            // Validate that the message is from the server (senderId can be null for server messages)
            // or from the correct player
            if (message.senderId != null && !player.getUUID().equals(message.senderId)) {
                LoggerBase.logError(null, "017001", "Received structure info message from sender " + message.senderId + " but expected " + player.getUUID());
                return;
            }

            // Validate structure count
            if (message.structures.size() > StructureInfoMessage.MAX_STRUCTURES) {
                LoggerBase.logError(null, "017002", "Received structure info message exceeding max count: " + message.structures.size() + " > " + StructureInfoMessage.MAX_STRUCTURES);
                return;
            }

            // Log the received message for now - in a real implementation you'd probably update client-side structure cache
            LoggerBase.logInfo(null, "017003", "Received structure info message with " + message.structures.size() + " structures from " + 
                (message.senderId != null ? "player " + player.getName().getString() : "server"));
        }
    }
}
