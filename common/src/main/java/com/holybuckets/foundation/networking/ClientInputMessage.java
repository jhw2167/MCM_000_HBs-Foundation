package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ClientInputMessage {
    public static final String LOCATION = "client_input";
    public final UUID playerId;
    public final InputType inputType;
    public final int code;

    public static enum InputType {
        KEY,
        MOUSE
    }

    ClientInputMessage(UUID playerId, InputType inputType, int code) {
        this.playerId = playerId;
        this.inputType = inputType;
        this.code = code;
    }

    public static void createAndFire(InputType type, int code) {
        //Get Player from the system
        Player p = Minecraft.getInstance().player;
        HBUtil.NetworkUtil.clientSendToServer( new ClientInputMessage(p.getUUID(), type, code));
    }
}
