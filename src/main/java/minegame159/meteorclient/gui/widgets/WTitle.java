/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.render.color.Color;

public class WTitle extends WWidget {
    public Color color;

    private final String text;

    public WTitle(String text) {
        this.text = text;
        this.color = GuiConfig.get().windowHeaderText;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = renderer.titleWidth(text);
        height = renderer.titleHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.title(text, x, y, color);
    }
}
