/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class Input {
    private static final boolean[] keys = new boolean[512];

    public static void setKeyState(int key, boolean pressed) {
        if (key < keys.length) keys[key] = pressed;
    }

    public static boolean isPressed(KeyBinding keyBinding) {
        int key = KeyBindingHelper.getBoundKeyOf(keyBinding).getCode();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isPressed(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < keys.length && keys[key];
    }
}
