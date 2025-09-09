/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.meteor;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.input.KeyInput;

public class KeyEvent extends Cancellable {
    private static final KeyEvent INSTANCE = new KeyEvent();

    public int key, modifiers;
    public KeyAction action;
    public KeyInput arg;

    // todo clean this up
    public static KeyEvent get(KeyInput arg, int key, int modifiers, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.arg = arg;
        INSTANCE.key = key;
        INSTANCE.modifiers = modifiers;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
