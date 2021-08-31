/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public record Word(String text, Color color) {
    public int width() {
        return mc.textRenderer.getWidth(text);
    }
}
