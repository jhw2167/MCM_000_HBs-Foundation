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
    public final int code;
    public final LevelNameSpace side;

    public static enum InputType {
        KEY,
        MOUSE
    }

    ClientInputMessage(UUID playerId, InputType inputType, int code, LevelNameSpace side) {
        this.playerId = playerId;
        this.inputType = inputType;
        this.code = code;
        this.side = side;
    }

    ClientInputMessage(ClientInputMessage other, LevelNameSpace side) {
        this.playerId = other.playerId;
        this.inputType = other.inputType;
        this.code = other.code;
        this.side = side;
    }




        public static void createAndFire(InputType type, int code) {
        //Get Player from the system
        Player p = Minecraft.getInstance().player;
        ClientInputMessage clientMessage = new ClientInputMessage(p.getUUID(), type, code, LevelNameSpace.CLIENT);
        EventRegistrar.getInstance().onClientInput(clientMessage);
        ClientInputMessage serverMessage = new ClientInputMessage(clientMessage, LevelNameSpace.SERVER);
        HBUtil.NetworkUtil.clientSendToServer( serverMessage );
    }
}
