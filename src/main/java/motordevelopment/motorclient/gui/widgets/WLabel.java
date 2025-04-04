/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.widgets;

import motordevelopment.motorclient.gui.widgets.pressable.WPressable;
import motordevelopment.motorclient.utils.render.color.Color;

public abstract class WLabel extends WPressable {
    public Color color;

    protected String text;
    protected boolean title;

    public WLabel(String text, boolean title) {
        this.text = text;
        this.title = title;
    }

    @Override
    protected void onCalculateSize() {
        width = theme.textWidth(text, text.length(), title);
        height = theme.textHeight(title);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (action != null) return super.onMouseClicked(mouseX, mouseY, button, used);
        return false;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (action != null) return super.onMouseReleased(mouseX, mouseY, button);
        return false;
    }

    public void set(String text) {
        if (Math.round(theme.textWidth(text, text.length(), title)) != width) invalidate();

        this.text = text;
    }

    public String get() {
        return text;
    }

    public WLabel color(Color color) {
        this.color = color;
        return this;
    }
}
