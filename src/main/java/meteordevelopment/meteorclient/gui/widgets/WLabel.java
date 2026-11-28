/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WLabel extends WPressable {
    public static final double MIN_TEXT_SCALE = 0.7;

    public Color color;
    public double maxWidth;
    public double textScale = 1;

    protected String text;
    private String displayText;
    protected boolean title;

    public WLabel(String text, boolean title) {
        this.text = text;
        this.title = title;
    }

    @Override
    protected void onCalculateSize() {
        width = theme.textWidth(text, text.length(), title);

        // Zu lang fuer die verfuegbare Breite, dann lieber kleiner als abgeschnitten
        displayText = null;

        if (maxWidth > 0 && width > maxWidth) {
            textScale = Math.max(MIN_TEXT_SCALE, maxWidth / width);
            width *= textScale;

            // Selbst verkleinert noch zu breit, dann kuerzen
            if (width > maxWidth) {
                displayText = truncate(maxWidth / textScale);
                width = theme.textWidth(displayText, displayText.length(), title) * textScale;
            }
        }

        height = theme.textHeight(title);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        if (action != null) return super.onMouseClicked(click, doubled);
        return false;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click) {
        if (action != null) return super.onMouseReleased(click);
        return false;
    }

    public void set(String text) {
        if (Math.round(theme.textWidth(text, text.length(), title)) != width) invalidate();

        this.text = text;
    }

    public String get() {
        return text;
    }

    public String getDisplay() {
        return displayText == null ? text : displayText;
    }

    private String truncate(double maxWidth) {
        double ellipsisWidth = theme.textWidth("…", 1, title);

        for (int length = text.length() - 1; length > 0; length--) {
            if (theme.textWidth(text, length, title) + ellipsisWidth <= maxWidth) {
                return text.substring(0, length).stripTrailing() + "…";
            }
        }

        return "…";
    }

    public WLabel color(Color color) {
        this.color = color;
        return this;
    }
}
