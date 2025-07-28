package com.holybuckets.foundation.client;

import com.holybuckets.foundation.networking.ClientInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientInput {
    public static void handleKeyPress(int keyCode) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            new ClientInputMessage(player.getUUID(), "KEY", keyCode).send();
        }
    }

    public static void handleMouseClick(int button) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            new ClientInputMessage(player.getUUID(), "MOUSE", button).send();
        }
    }
}
