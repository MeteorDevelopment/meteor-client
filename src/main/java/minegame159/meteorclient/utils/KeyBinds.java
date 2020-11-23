/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {
    private static final String CATEGORY = "Meteor Client";

    public static KeyBinding OPEN_CLICK_GUI = new KeyBinding("key.meteor-client.open-click-gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY);
    public static KeyBinding SHULKER_PEEK = new KeyBinding("key.meteor-client.shulker-peek", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    public static void Register() {
        KeyBindingHelper.registerKeyBinding(OPEN_CLICK_GUI);
        KeyBindingHelper.registerKeyBinding(SHULKER_PEEK);
    }
}
