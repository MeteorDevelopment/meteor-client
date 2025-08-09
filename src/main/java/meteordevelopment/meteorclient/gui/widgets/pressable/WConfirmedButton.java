/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;

public abstract class WConfirmedButton extends WButton {

    protected boolean pressedOnce = false;
    protected String confirmText;

    public WConfirmedButton(String text, String confirmText, GuiTexture texture) {
        super(text, texture);
        this.confirmText = confirmText;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        String text = getText();

        if (text != null) {
            textWidth = theme.textWidth(text);

            width = pad + textWidth + pad;
            height = pad + theme.textHeight() + pad;
        }
        else {
            double s = theme.textHeight();

            width = pad + s + pad;
            height = pad + s + pad;
        }
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        boolean pressed = super.onMouseClicked(mouseX, mouseY, button, used);
        if (!pressed) {
            pressedOnce = false;
            invalidate();
        }
        return pressed;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (pressed && pressedOnce) super.onMouseReleased(mouseX, mouseY, button);
        pressedOnce = pressed;
        invalidate();
        return pressed = false;
    }

    public String getText() {
        return pressedOnce ? confirmText : text;
    }
}
