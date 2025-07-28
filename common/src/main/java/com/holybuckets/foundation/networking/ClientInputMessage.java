package com.holybuckets.foundation.networking;

import java.util.UUID;

public class ClientInputMessage {
    public final UUID playerId;
    public final String inputType;
    public final int code;

    public ClientInputMessage(UUID playerId, String inputType, int code) {
        this.playerId = playerId;
        this.inputType = inputType;
        this.code = code;
    }

    public void send() {
        // Implementation will depend on your networking system
        // This would use your mod's network channel to send to server
    }
}
