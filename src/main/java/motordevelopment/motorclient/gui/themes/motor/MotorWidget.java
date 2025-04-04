/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor;

import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.utils.BaseWidget;
import motordevelopment.motorclient.gui.widgets.WWidget;
import motordevelopment.motorclient.utils.render.color.Color;

public interface MotorWidget extends BaseWidget {
    default MotorGuiTheme theme() {
        return (MotorGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        MotorGuiTheme theme = theme();
        double s = theme.scale(2);

        renderer.quad(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, theme.backgroundColor.get(pressed, mouseOver));

        Color outlineColor = theme.outlineColor.get(pressed, mouseOver);
        renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);
    }
}
