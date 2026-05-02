package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import static com.holybuckets.foundation.FoundationInitializers.id;
import static com.holybuckets.foundation.HBUtil.LevelUtil.LevelNameSpace;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientInputMessage implements CustomPacketPayload {

    public static final String LOCATION = "client_input";
    public static final int MAX_KEYS = 5;

    public static final CustomPacketPayload.Type<ClientInputMessage> TYPE =
        new CustomPacketPayload.Type<>(id(LOCATION));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientInputMessage> STREAM_CODEC =
        CustomPacketPayload.codec(Codecs::encodeClientInput, Codecs::decodeClientInput);

    public final UUID playerId;
    public final InputType inputType;
    public final int code;
    public final Set<Integer> keyCodes;
    public final LevelNameSpace side;

    public enum InputType { KEY, MOUSE }

    ClientInputMessage(UUID playerId, InputType inputType, Set<Integer> keyCodes, LevelNameSpace side) {
        this.playerId = playerId;
        this.inputType = inputType;
        this.keyCodes = new HashSet<>(keyCodes);
        this.code = keyCodes.isEmpty() ? -1 : keyCodes.iterator().next();
        this.side = side;
    }

    ClientInputMessage(ClientInputMessage other, LevelNameSpace side) {
        this.playerId = other.playerId;
        this.inputType = other.inputType;
        this.keyCodes = new HashSet<>(other.keyCodes);
        this.code = other.code;
        this.side = side;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void createAndFire(InputType type, Set<Integer> keyCodes, Player p) {
        Set<Integer> limitedKeys = keyCodes.stream().limit(MAX_KEYS).collect(Collectors.toSet());
        ClientInputMessage clientMessage = new ClientInputMessage(p.getUUID(), type, limitedKeys, LevelNameSpace.CLIENT);
        ClientEventRegistrar.getInstance().onClientInput(clientMessage);
        ClientInputMessage serverMessage = new ClientInputMessage(clientMessage, LevelNameSpace.SERVER);
        HBUtil.NetworkUtil.clientSendToServer(serverMessage);
    }
}