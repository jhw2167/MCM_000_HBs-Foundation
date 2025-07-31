package com.holybuckets.foundation.client;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ClientTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.balm.api.event.EventPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.KeyMapping;

import java.util.HashSet;
import java.util.Set;

public class ClientInput {

    public static void init(EventRegistrar reg) {
        // Register the client input handler
        reg.registerOnClientTick(TickType.ON_SINGLE_TICK, ClientInput::onClientTick, EventPriority.Highest);
    }

    private static int prevInput = -1;
    
    public static void onClientTick(ClientTickEvent event) {

        Set<Integer> currentInputs = new HashSet<>();
        prevInput = collectKeys(currentInputs);
        if (prevInput == -1) return;

        handleKeyPresses(currentInputs);

    }

    public static void handleKeyPresses(Set<Integer> keyCodes) {
        ClientInputMessage.createAndFire(ClientInputMessage.InputType.KEY, keyCodes);
    }


    static int collectKeys(Set<Integer> keys) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return -1;
        }

        if (client.options.keyAttack.isDown()) {
            keys.add(InputConstants.MOUSE_BUTTON_LEFT);
        }
        if (client.options.keyUse.isDown()) {
            keys.add(InputConstants.MOUSE_BUTTON_RIGHT);
        }
        if (client.options.keyJump.isDown()) {
            keys.add(InputConstants.KEY_SPACE);
        }

        // Movement keys
        if (player.input.up) {
            keys.add(InputConstants.KEY_W);
        }
        if (player.input.down) {
            keys.add(InputConstants.KEY_S);
        }
        if (player.input.left) {
            keys.add(InputConstants.KEY_A);
        }
        if (player.input.right) {
            keys.add(InputConstants.KEY_D);
        }

        // Other keys
        if (client.options.keyInventory.isDown()) {
            keys.add(InputConstants.KEY_E);
        }
        if (client.options.keyShift.isDown()) {
            keys.add(InputConstants.KEY_LSHIFT);
        }
        if (client.options.keyDrop.isDown()) {
            keys.add(InputConstants.KEY_Q);
        }

        return keys.isEmpty() ? -1 : keys.iterator().next();
    }

}
