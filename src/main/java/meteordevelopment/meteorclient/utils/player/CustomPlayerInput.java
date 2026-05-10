/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class CustomPlayerInput extends ClientInput {
    @Override
    public void tick() {
        float f = this.keyPresses.forward() == this.keyPresses.backward() ? 0.0F : (this.keyPresses.forward() ? 1.0F : -1.0F);
        float g = this.keyPresses.left() == this.keyPresses.right() ? 0.0F : (this.keyPresses.left() ? 1.0F : -1.0F);
        this.moveVector = new Vec2(g, f).normalized();
    }

    public void stop() {
        this.keyPresses = Input.EMPTY;
    }

    public void forward(boolean bool) {
        this.keyPresses = new Input(
            bool,
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }

    public void backward(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            bool,
            this.keyPresses.left(),
            this.keyPresses.right(),
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }

    public void left(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            bool,
            this.keyPresses.right(),
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }

    public void right(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            bool,
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }

    public void jump(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            bool,
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }

    public void sneak(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            this.keyPresses.jump(),
            bool,
            this.keyPresses.sprint()
        );
    }

    public void sprint(boolean bool) {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            bool
        );
    }
}
