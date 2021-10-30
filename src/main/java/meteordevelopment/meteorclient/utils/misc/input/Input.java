/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.input;

import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.utils.misc.CursorStyle;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];

    private static CursorStyle lastCursorStyle = CursorStyle.Default;

    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static void setKeyState(KeyBinding bind, boolean pressed) {
        setKeyState(KeyBinds.getKey(bind), pressed);
    }

    public static boolean isPressed(KeyBinding bind) {
        return isKeyPressed(KeyBinds.getKey(bind));
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
}
