/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import net.minecraft.client.input.MouseButtonEvent;

public class WConfirmedMinus extends WMinus {
    protected boolean pressedOnce = false;

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        boolean pressed = super.onMouseClicked(click, doubled);
        if (!pressed) {
            pressedOnce = false;
        }
        return pressed;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click) {
        if (pressed && pressedOnce) super.onMouseReleased(click);
        pressedOnce = pressed;
        return pressed = false;
    }
}
