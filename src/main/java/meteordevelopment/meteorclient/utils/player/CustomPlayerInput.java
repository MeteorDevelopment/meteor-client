/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public class CustomPlayerInput extends Input {
    @Override
    public void tick() {
        float f = this.playerInput.forward() == this.playerInput.backward() ? 0.0F : (this.playerInput.forward() ? 1.0F : -1.0F);
        float g = this.playerInput.left() == this.playerInput.right() ? 0.0F : (this.playerInput.left() ? 1.0F : -1.0F);
        this.movementVector = new Vec2f(g, f).normalize();
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
