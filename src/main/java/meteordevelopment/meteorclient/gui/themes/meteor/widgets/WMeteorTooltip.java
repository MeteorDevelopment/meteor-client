/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorTooltip extends WTooltip implements MeteorWidget {
    public WMeteorTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Color bg = theme().backgroundColor.get();
        Color accent = theme().accentColor.get();

        // Modern tooltip with gradient and accent border
        Color bgDark = new Color(bg.r - 5, bg.g - 5, bg.b, Math.min(255, bg.a + 30));
        Color bgLight = new Color(bg.r + 10, bg.g + 10, bg.b + 15, Math.min(255, bg.a + 30));

        renderer.quad(x, y, width, height, bgLight, bgLight, bgDark, bgDark);

        // Accent border on left
        renderer.quad(x, y, 2, height, accent);

        // Subtle top highlight
        Color highlight = new Color(255, 255, 255, 25);
        renderer.quad(x + 2, y, width - 2, 1, highlight);
    }
}
