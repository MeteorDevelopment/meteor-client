/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.render.color.Color;

public class WPlus extends WPressable {
    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 6 + renderer.textHeight() + 6;
        height = 6 + renderer.textHeight() + 6;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.background(this, mouseOver, pressed);

        Color color = GuiConfig.get().plus;
        if (pressed) color = GuiConfig.get().plusPressed;
        else if (mouseOver) color = GuiConfig.get().plusHovered;

        renderer.quad(Region.FULL, x + 7, y + 6 + renderer.textHeight() / 2 - 1, renderer.textHeight() - 1, 3, color);
        renderer.quad(Region.FULL, x + 6 + renderer.textHeight() / 2 - 1, y + 7, 3, renderer.textHeight() - 1, color);
    }
}
