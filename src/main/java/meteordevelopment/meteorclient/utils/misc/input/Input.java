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

    private static CursorType lastCursorStyle = CursorTypes.ARROW;

    private static final int[] MODIFIER_GROUPS = {InputConstants.MOD_SHIFT, InputConstants.MOD_CONTROL, InputConstants.MOD_ALT, InputConstants.MOD_SUPER};

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

    public static void setKeyState(KeyMapping bind, boolean pressed) {
        setKeyState(getKey(bind), pressed);
    }

    public static boolean isPressed(KeyMapping bind) {
        return isKeyPressed(getKey(bind)) || isButtonPressed(getKey(bind));
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key == InputConstants.UNKNOWN.getValue()) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return button < buttons.length && buttons[button];
    }

    public static void setCursorStyle(CursorType style) {
        if (lastCursorStyle != style) {
            style.select();
            lastCursorStyle = style;
        }
    }

    /**
     * Whether exactly the modifiers in {@code mask} are held. SDL reports the left and right
     * key of each modifier as separate bits and carries the Num Lock and Caps Lock state in
     * every event, so comparing {@code modifiers} directly against a {@code MOD_*} constant
     * never matches.
     */
    public static boolean modifiersMatch(int modifiers, int mask) {
        modifiers &= ~(InputConstants.MOD_CAPS_LOCK | InputConstants.MOD_NUM_LOCK);

        for (int group : MODIFIER_GROUPS) {
            if ((mask & group) != 0 && (modifiers & group) == 0) return false;
        }

        return (modifiers & ~mask) == 0;
    }

    public static int getModifier(int key) {
        return switch (key) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> InputConstants.MOD_SHIFT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> InputConstants.MOD_CONTROL;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> InputConstants.MOD_ALT;
            case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> InputConstants.MOD_SUPER;
            default -> 0;
        };
    }
}
