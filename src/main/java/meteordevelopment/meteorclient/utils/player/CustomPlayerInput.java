/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CustomPlayerInput extends Input {
    @Override
    public void tick(boolean slowDown, float f) {
        movementForward = mc.player.input.playerInput.forward() == mc.player.input.playerInput.backward() ? 0.0F : (mc.player.input.playerInput.forward() ? 1.0F : -1.0F);
        movementSideways = mc.player.input.playerInput.left() == mc.player.input.playerInput.right() ? 0.0F : (mc.player.input.playerInput.left() ? 1.0F : -1.0F);

        if (mc.player.isSneaking()) {
            movementForward *= 0.3;
            movementSideways *= 0.3;
        }
    }

    public void stop() {
        mc.player.input.playerInput = PlayerInput.DEFAULT;
    }
}
