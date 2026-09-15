/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super(Categories.Movement, "no-jump-delay", "Removes the cooldown between jumps");
    }
}
