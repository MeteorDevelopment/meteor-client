/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorWindow extends WWindow implements MeteorWidget {
    public WMeteorWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WMeteorHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            Color bg = theme().backgroundColor.get();
            Color accent = theme().accentColor.get();

            // Dark glass background with more transparency
            Color bgMain = new Color(bg.r, bg.g, bg.b, 180);
            renderer.quad(x, y + header.height, width, height - header.height, bgMain);

            // Inner glow from top (accent colored)
            Color glowTop = new Color(accent.r, accent.g, accent.b, 40);
            Color glowFade = new Color(accent.r, accent.g, accent.b, 0);
            renderer.quad(x, y + header.height, width, 20, glowTop, glowTop, glowFade, glowFade);

            // Left accent bar with glow
            renderer.quad(x, y + header.height, 3, height - header.height, accent);
            renderer.quad(x + 3, y + header.height, 8, height - header.height, glowTop, glowFade, glowFade, glowTop);

            // Bottom subtle line
            Color bottomLine = new Color(accent.r, accent.g, accent.b, 60);
            renderer.quad(x, y + height - 2, width, 2, bottomLine);
        }
    }

    private class WMeteorHeader extends WHeader {
        public WMeteorHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Color accent = theme().accentColor.get();

            // Vibrant gradient header
            Color accentBright = new Color(Math.min(255, accent.r + 50), Math.min(255, accent.g + 30), Math.min(255, accent.b + 10), 255);
            Color accentDark = new Color((int)(accent.r * 0.5), (int)(accent.g * 0.4), (int)(accent.b * 0.3), 255);

            renderer.quad(x, y, width, height, accentBright, accentBright, accentDark, accentDark);

            // Strong top highlight
            Color highlight = new Color(255, 255, 255, 80);
            renderer.quad(x, y, width, 2, highlight);

            // Bottom glow into content
            Color glowDown = new Color(accent.r, accent.g, accent.b, 50);
            Color glowFade = new Color(accent.r, accent.g, accent.b, 0);
            renderer.quad(x, y + height, width, 6, glowDown, glowDown, glowFade, glowFade);
        }
    }
}
