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
    public void tick() {
        movementForward = mc.player.input.playerInput.forward() == mc.player.input.playerInput.backward() ? 0.0F : (mc.player.input.playerInput.forward() ? 1.0F : -1.0F);
        movementSideways = mc.player.input.playerInput.left() == mc.player.input.playerInput.right() ? 0.0F : (mc.player.input.playerInput.left() ? 1.0F : -1.0F);

        if (mc.player.isSneaking()) {
            movementForward *= 0.3;
            movementSideways *= 0.3;
        }
    }

    public void stop() {
        this.playerInput = PlayerInput.DEFAULT;
    }

    public void forward(boolean bool) {
        this.playerInput = new PlayerInput(
            bool,
            this.playerInput.backward(),
            this.playerInput.left(),
            this.playerInput.right(),
            this.playerInput.jump(),
            this.playerInput.sneak(),
            this.playerInput.sprint()
        );
    }

    public void backward(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            bool,
            this.playerInput.left(),
            this.playerInput.right(),
            this.playerInput.jump(),
            this.playerInput.sneak(),
            this.playerInput.sprint()
        );
    }

    public void left(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            this.playerInput.backward(),
            bool,
            this.playerInput.right(),
            this.playerInput.jump(),
            this.playerInput.sneak(),
            this.playerInput.sprint()
        );
    }

    public void right(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            this.playerInput.backward(),
            this.playerInput.left(),
            bool,
            this.playerInput.jump(),
            this.playerInput.sneak(),
            this.playerInput.sprint()
        );
    }

    public void jump(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            this.playerInput.backward(),
            this.playerInput.left(),
            this.playerInput.right(),
            bool,
            this.playerInput.sneak(),
            this.playerInput.sprint()
        );
    }

    public void sneak(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            this.playerInput.backward(),
            this.playerInput.left(),
            this.playerInput.right(),
            this.playerInput.jump(),
            bool,
            this.playerInput.sprint()
        );
    }

    public void sprint(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.forward(),
            this.playerInput.backward(),
            this.playerInput.left(),
            this.playerInput.right(),
            this.playerInput.jump(),
            this.playerInput.sneak(),
            bool
        );
    }
}
