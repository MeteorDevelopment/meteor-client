/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import org.lwjgl.glfw.GLFW;

public enum KeyAction {
    Press,
    Repeat,
    Release;

    public static KeyAction get(int action) {
        if (action == GLFW.GLFW_PRESS) return Press;
        else if (action == GLFW.GLFW_RELEASE) return Release;
        else return Repeat;
    }
}
