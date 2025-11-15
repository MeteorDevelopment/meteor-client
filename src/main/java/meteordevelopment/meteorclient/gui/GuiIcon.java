/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.utils.render.color.Color;

import javax.annotation.Nullable;

public class GuiIcon {

    public GuiTexture texture;
    public double rotation;
    public @Nullable Color color;

    public GuiIcon(GuiTexture texture, double rotation, @Nullable Color color) {
        this.texture = texture;
        this.rotation = rotation;
        this.color = color;
    }
    public GuiIcon(GuiTexture texture) {
        this(texture, 0, null);
    }

    public GuiIcon(GuiTexture texture, double rotation) {
        this(texture, rotation, null);
    }

    public GuiIcon(GuiTexture texture, Color color) {
        this(texture, 0, color);
    }
}
