/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.themes.meteor;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.utils.BaseWidget;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.render.color.Color;

public interface MeteorWidget extends BaseWidget {
    default MeteorGuiTheme theme() {
        return (MeteorGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        MeteorGuiTheme theme = theme();
        int r = theme().round.get();
        double s = theme.scale(2);
        Color outlineColor = theme.outlineColor.get(pressed, mouseOver);
        if (r == 0) {
            renderer.quad(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, theme.backgroundColor.get(pressed, mouseOver));

            renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
            renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
            renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
            renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);
        }
        else {
            renderer.quadRounded(widget.x, widget.y, widget.width, widget.height, outlineColor, r);
            renderer.quadRounded(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, theme.backgroundColor.get(pressed, mouseOver), r);
        }
    }
}
