/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.input;

import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.utils.misc.CursorStyle;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];

    private static CursorStyle lastCursorStyle = CursorStyle.Default;

    private Input() {
    }

    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static int getKey(KeyBinding bind) {
        return ((KeyBindingAccessor) bind).getKey().getCode();
    }

    public static void setKeyState(KeyBinding bind, boolean pressed) {
        setKeyState(getKey(bind), pressed);
    }

    public static boolean isPressed(KeyBinding bind) {
        return isKeyPressed(getKey(bind));
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return button < buttons.length && buttons[button];
    }

    public static void setCursorStyle(CursorStyle style) {
        if (lastCursorStyle != style) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), style.getGlfwCursor());
            lastCursorStyle = style;
        }
    }

    public static int getModifier(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> GLFW.GLFW_MOD_SUPER;
            default -> 0;
        };
    }
}
