/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];

    private static final int MOD_KEYS = InputConstants.MOD_SHIFT
        | InputConstants.MOD_CONTROL
        | InputConstants.MOD_ALT
        | InputConstants.MOD_SUPER;

    private static CursorType lastCursorStyle = CursorTypes.ARROW;

    private Input() {
    }

    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static int getKey(KeyMapping bind) {
        return ((KeyMappingAccessor) bind).meteor$getKey().getValue();
    }

    public static boolean matches(KeyMapping bind, InputConstants.Key key) {
        // SDL keyboard scancodes and mouse buttons can have the same numeric value.
        return ((KeyMappingAccessor) bind).meteor$getKey().equals(key);
    }

    public static void setKeyState(KeyMapping bind, boolean pressed) {
        InputConstants.Key key = ((KeyMappingAccessor) bind).meteor$getKey();
        if (key.getType() == InputConstants.Type.MOUSE) setButtonState(key.getValue(), pressed);
        else setKeyState(key.getValue(), pressed);
    }

    public static boolean isPressed(KeyMapping bind) {
        InputConstants.Key key = ((KeyMappingAccessor) bind).meteor$getKey();
        return key.getType() == InputConstants.Type.MOUSE
            ? isButtonPressed(key.getValue())
            : isKeyPressed(key.getValue());
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key == InputConstants.UNKNOWN.getValue()) return false;
        return key >= 0 && key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return button >= 0 && button < buttons.length && buttons[button];
    }

    public static void setCursorStyle(CursorType style) {
        if (lastCursorStyle != style) {
            style.select();
            lastCursorStyle = style;
        }
    }

    public static int getModifier(int key) {
        return switch (key) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> InputConstants.MOD_SHIFT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> InputConstants.MOD_CONTROL;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> InputConstants.MOD_ALT;
//            case InputConstants.KEY_LSUPER, InputConstants.KEY_RSUPER -> InputConstants.MOD_SUPER;
            default -> 0;
        };
    }

    public static boolean modsMatch(int currentMods, int... desiredMods) {
        int desiredMask = 0;

        for (int mod : desiredMods) {
            // a required modifier isn't held
            if ((currentMods & mod) == 0) return false;

            desiredMask |= mod;
        }

        // nothing extra is held, discounting capslock & numlock
        return (currentMods & MOD_KEYS & ~desiredMask) == 0;
    }
}
