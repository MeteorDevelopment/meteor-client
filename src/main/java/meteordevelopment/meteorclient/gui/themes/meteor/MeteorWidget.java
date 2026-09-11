/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface MeteorWidget extends BaseWidget {
    default MeteorGuiTheme theme() {
        return (MeteorGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        MeteorGuiTheme theme = theme();
        Color accent = theme.accentColor.get();

        // Glass-like background
        Color bgMain = new Color(backgroundColor.r, backgroundColor.g, backgroundColor.b, 160);
        renderer.quad(widget.x, widget.y, widget.width, widget.height, bgMain);

        // Top edge highlight
        Color highlight = new Color(255, 255, 255, 35);
        renderer.quad(widget.x, widget.y, widget.width, 1, highlight);

        // Bottom edge shadow
        Color shadow = new Color(0, 0, 0, 50);
        renderer.quad(widget.x, widget.y + widget.height - 1, widget.width, 1, shadow);

        // Left accent line
        Color accentLine = new Color(accent.r, accent.g, accent.b, 120);
        renderer.quad(widget.x, widget.y, 2, widget.height, accentLine);

        // Inner glow from left
        Color glowStart = new Color(accent.r, accent.g, accent.b, 20);
        Color glowEnd = new Color(accent.r, accent.g, accent.b, 0);
        renderer.quad(widget.x + 2, widget.y, 10, widget.height, glowStart, glowEnd, glowEnd, glowStart);
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        MeteorGuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
    }
}
