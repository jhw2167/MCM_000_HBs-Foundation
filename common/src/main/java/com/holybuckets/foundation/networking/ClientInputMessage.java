package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import static com.holybuckets.foundation.HBUtil.LevelUtil.LevelNameSpace;
import java.util.UUID;

public class ClientInputMessage {
    public static final String LOCATION = "client_input";
    public final UUID playerId;
    public final InputType inputType;
    public final int code; // Keeping for backwards compatibility
    public final Set<Integer> keyCodes;
    public final LevelNameSpace side;
    public static final int MAX_KEYS = 5;

    public static enum InputType {
        KEY,
        MOUSE
    }

    ClientInputMessage(UUID playerId, InputType inputType, Set<Integer> keyCodes, LevelNameSpace side) {
        this.playerId = playerId;
        this.inputType = inputType;
        this.keyCodes = new HashSet<>(keyCodes);
        this.code = keyCodes.isEmpty() ? -1 : keyCodes.iterator().next(); // Backwards compatibility
        this.side = side;
    }

    ClientInputMessage(ClientInputMessage other, LevelNameSpace side) {
        this.playerId = other.playerId;
        this.inputType = other.inputType;
        this.keyCodes = new HashSet<>(other.keyCodes);
        this.code = other.code;
        this.side = side;
    }

    public static void createAndFire(InputType type, Set<Integer> keyCodes) {
        Player p = Minecraft.getInstance().player;
        // Limit to MAX_KEYS
        Set<Integer> limitedKeys = keyCodes.stream()
            .limit(MAX_KEYS)
            .collect(Collectors.toSet());
        ClientInputMessage clientMessage = new ClientInputMessage(p.getUUID(), type, limitedKeys, LevelNameSpace.CLIENT);
        EventRegistrar.getInstance().onClientInput(clientMessage);
        ClientInputMessage serverMessage = new ClientInputMessage(clientMessage, LevelNameSpace.SERVER);
        HBUtil.NetworkUtil.clientSendToServer(serverMessage);
    }
}
