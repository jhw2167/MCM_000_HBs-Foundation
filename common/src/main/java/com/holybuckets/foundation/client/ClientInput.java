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

public class ClientInput {

    public static void init(EventRegistrar reg) {
        // Register the client input handler
        reg.registerOnClientTick(TickType.ON_SINGLE_TICK, ClientInput::onClientTick, EventPriority.Highest);
    }

    private static int prevInput = -1;
    public static void onClientTick(ClientTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        int input = toIntKey(client);

        if( input == -1 && prevInput == -1) return;

        handleKeyPress(input);
        prevInput = input;
    }

    public static void handleKeyPress(int keyCode) {
        ClientInputMessage.createAndFire(ClientInputMessage.InputType.KEY, keyCode);
    }


    static int toIntKey(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return -1; // No player is present
        }

        if (client.options.keyAttack.isDown()) {
            return InputConstants.MOUSE_BUTTON_LEFT;
        }
        if (client.options.keyUse.isDown()) {
            return InputConstants.MOUSE_BUTTON_RIGHT;
        }
        if (client.options.keyJump.isDown()) {
            return InputConstants.KEY_SPACE;
        }
        if (client.options.keySprint.isDown()) {
            //skip
        }

        // Check for movement keys
        if (player.input.up) {
            return InputConstants.KEY_W; // Forward
        }
        if (player.input.down) {
            return InputConstants.KEY_S; // Backward
        }
        if (player.input.left) {
            return InputConstants.KEY_A; // Left
        }
        if (player.input.right) {
            return InputConstants.KEY_D; // Right
        }
        // Check for other keys
        if (client.options.keyInventory.isDown()) {
            return InputConstants.KEY_E; // Inventory
        }
        if (client.options.keyShift.isDown()) {
            return InputConstants.KEY_LSHIFT; // Left Arrow
        }
        if (client.options.keyDrop.isDown()) {
            return InputConstants.KEY_Q; // Drop
        }



        return -1;
    }

}
