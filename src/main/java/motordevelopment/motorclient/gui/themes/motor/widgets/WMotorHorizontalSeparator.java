/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor.widgets;

import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.themes.motor.MotorGuiTheme;
import motordevelopment.motorclient.gui.themes.motor.MotorWidget;
import motordevelopment.motorclient.gui.widgets.WHorizontalSeparator;

public class WMotorHorizontalSeparator extends WHorizontalSeparator implements MotorWidget {
    public WMotorHorizontalSeparator(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (text == null) renderWithoutText(renderer);
        else renderWithText(renderer);
    }

    private void renderWithoutText(GuiRenderer renderer) {
        MotorGuiTheme theme = theme();
        double s = theme.scale(1);
        double w = width / 2;

        renderer.quad(x, y + s, w, s, theme.separatorEdges.get(), theme.separatorCenter.get());
        renderer.quad(x + w, y + s, w, s, theme.separatorCenter.get(), theme.separatorEdges.get());
    }

    private void renderWithText(GuiRenderer renderer) {
        MotorGuiTheme theme = theme();
        double s = theme.scale(2);
        double h = theme.scale(1);

        double textStart = Math.round(width / 2.0 - textWidth / 2.0 - s);
        double textEnd = s + textStart + textWidth + s;

        double offsetY = Math.round(height / 2.0);

        renderer.quad(x, y + offsetY, textStart, h, theme.separatorEdges.get(), theme.separatorCenter.get());
        renderer.text(text, x + textStart + s, y, theme.separatorText.get(), false);
        renderer.quad(x + textEnd, y + offsetY, width - textEnd, h, theme.separatorCenter.get(), theme.separatorEdges.get());
    }
}
