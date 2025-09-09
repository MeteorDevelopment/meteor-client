/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.meteor;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.gui.Click;

public class MouseButtonEvent extends Cancellable {
    private static final MouseButtonEvent INSTANCE = new MouseButtonEvent();

    public int button;
    public KeyAction action;
    public Click arg;

    // todo cleanup
    public static MouseButtonEvent get(Click arg, int button, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.arg = arg;
        INSTANCE.button = button;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
