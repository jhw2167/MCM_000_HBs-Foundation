package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.networking.ClientInputMessage;
import net.minecraft.world.entity.player.Player;
import java.util.Set;

public class ClientInputEvent {
    private final Player player;
    private final ClientInputMessage message;

    public ClientInputEvent(Player player, ClientInputMessage message) {
        this.player = player;
        this.message = message;
    }

    public Player getPlayer() {
        return player;
    }

    public ClientInputMessage getMessage() {
        return message;
    }

    public Set<Integer> getKeyCodes() {
        return message.keyCodes;
    }

    public int getLegacyKeyCode() {
        return message.code;
    }
}
