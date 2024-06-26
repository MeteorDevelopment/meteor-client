/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.meteor;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;

public class KeyEvent extends Cancellable {
    private static final KeyEvent INSTANCE = new KeyEvent();

    public int key, modifiers;
    public KeyAction action;

    public static KeyEvent get(int key, int modifiers, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.key = key;
        INSTANCE.modifiers = modifiers;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
