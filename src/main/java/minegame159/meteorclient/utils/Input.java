package minegame159.meteorclient.utils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;

public class Input {
    private static final boolean[] keys = new boolean[512];

    public static void setKeyState(int key, boolean pressed) {
        if (key < keys.length) keys[key] = pressed;
    }

    public static boolean isPressed(KeyBinding keyBinding) {
        int key = KeyBindingHelper.getBoundKeyOf(keyBinding).getCode();
        return key < keys.length && keys[key];
    }

    public static boolean isPressed(int key) {
        return key < keys.length && keys[key];
    }
}
