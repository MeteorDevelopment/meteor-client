/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor.widgets.pressable;

import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.themes.motor.MotorWidget;
import motordevelopment.motorclient.gui.widgets.pressable.WTriangle;

public class WMotorTriangle extends WTriangle implements MotorWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().backgroundColor.get(pressed, mouseOver));
    }
}
