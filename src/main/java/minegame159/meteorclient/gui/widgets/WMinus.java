/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.Color;

public class WMinus extends WPressable {
    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 6 + renderer.textHeight() + 6;
        height = 6 + renderer.textHeight() + 6;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.background(this, mouseOver, pressed);

        Color color = GuiConfig.INSTANCE.minus;
        if (pressed) color = GuiConfig.INSTANCE.minusPressed;
        else if (mouseOver) color = GuiConfig.INSTANCE.minusHovered;

        renderer.quad(Region.FULL, x + 6, y + 6 + renderer.textHeight() / 2 - 1.5, renderer.textHeight(), 3, color);
    }
}
