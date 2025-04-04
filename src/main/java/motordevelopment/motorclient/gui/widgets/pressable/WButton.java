/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.widgets.pressable;

import motordevelopment.motorclient.gui.renderer.packer.GuiTexture;

public abstract class WButton extends WPressable {
    protected String text;
    protected double textWidth;

    protected GuiTexture texture;

    public WButton(String text, GuiTexture texture) {
        this.text = text;
        this.texture = texture;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

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

    public void set(String text) {
        if (this.text == null || Math.round(theme.textWidth(text)) != textWidth) invalidate();

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
