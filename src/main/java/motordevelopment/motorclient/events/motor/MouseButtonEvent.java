/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.motor;

import motordevelopment.motorclient.events.Cancellable;
import motordevelopment.motorclient.utils.misc.input.KeyAction;

public class MouseButtonEvent extends Cancellable {
    private static final MouseButtonEvent INSTANCE = new MouseButtonEvent();

    public int button;
    public KeyAction action;

    public static MouseButtonEvent get(int button, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.button = button;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
