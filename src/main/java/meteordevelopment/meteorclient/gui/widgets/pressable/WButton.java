/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.GuiIcon;

public abstract class WButton extends WPressable {
    protected String text;
    protected double textWidth;

    protected GuiIcon icon;

    public WButton(String text, GuiIcon icon) {
        this.text = text;
        this.icon = icon;

        if (text == null) instantTooltips = true;
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

    public void set(String text) {
        if (this.text == null || Math.round(theme.textWidth(text)) != textWidth) invalidate();

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
