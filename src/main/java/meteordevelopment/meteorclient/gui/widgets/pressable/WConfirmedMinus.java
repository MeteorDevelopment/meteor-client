/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

public class WConfirmedMinus extends WMinus {
    protected boolean pressedOnce = false;

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        boolean pressed = super.onMouseClicked(mouseX, mouseY, button, used);
        if (!pressed) {
            pressedOnce = false;
        }
        return pressed;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (pressed && pressedOnce) super.onMouseReleased(mouseX, mouseY, button);
        pressedOnce = pressed;
        return pressed = false;
    }
}
