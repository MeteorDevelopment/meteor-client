/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor.widgets;

import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.themes.motor.MotorWidget;
import motordevelopment.motorclient.gui.widgets.WWidget;
import motordevelopment.motorclient.gui.widgets.containers.WWindow;

public class WMotorWindow extends WWindow implements MotorWidget {
    public WMotorWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WMotorHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());
        }
    }

    private class WMotorHeader extends WHeader {
        public WMotorHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.quad(this, theme().accentColor.get());
        }
    }
}
