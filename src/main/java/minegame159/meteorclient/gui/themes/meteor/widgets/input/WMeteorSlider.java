/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.themes.meteor.widgets.input;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import minegame159.meteorclient.gui.themes.meteor.MeteorWidget;
import minegame159.meteorclient.gui.widgets.input.WSlider;

public class WMeteorSlider extends WSlider implements MeteorWidget {
    public WMeteorSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double valueWidth = valueWidth();

        renderBar(renderer, valueWidth);
        renderHandle(renderer, valueWidth);
    }

    private void renderBar(GuiRenderer renderer, double valueWidth) {
        MeteorGuiTheme theme = theme();

        double s = theme.scale(3);
        double handleSize = handleSize();

        double x = this.x + handleSize / 2;
        double y = this.y + height / 2 - s / 2;

        renderer.quad(x, y, valueWidth, s, theme.sliderLeft.get());
        renderer.quad(x + valueWidth, y, width - valueWidth - handleSize, s, theme.sliderRight.get());
    }

    private void renderHandle(GuiRenderer renderer, double valueWidth) {
        MeteorGuiTheme theme = theme();
        double s = handleSize();

        renderer.quad(x + valueWidth, y, s, s, GuiRenderer.CIRCLE, theme.sliderHandle.get(dragging, handleMouseOver));
    }
}
