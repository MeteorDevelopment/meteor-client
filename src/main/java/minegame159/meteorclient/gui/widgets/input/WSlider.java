/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets.input;

import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Utils;

public abstract class WSlider extends WWidget {
    public Runnable action;
    public Runnable actionOnRelease;

    protected double value;
    protected double min, max;

    protected boolean handleMouseOver;
    protected boolean dragging;
    protected double valueAtDragStart;

    public WSlider(double value, double min, double max) {
        this.value = Utils.clamp(value, min, max);
        this.min = min;
        this.max = max;
    }

    protected double handleSize() {
        return theme.textHeight();
    }

    @Override
    protected void onCalculateSize() {
        double s = handleSize();

        width = s;
        height = s;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (mouseOver && !used) {
            valueAtDragStart = value;
            double handleSize = handleSize();

            double valueWidth = mouseX - (x + handleSize / 2);
            set((valueWidth / (width - handleSize)) * (max - min) + min);
            if (action != null) action.run();

            dragging = true;
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        double valueWidth = valueWidth();
        double s = handleSize();
        double s2 = s / 2;

        double x = this.x + s2 + valueWidth - height / 2;
        handleMouseOver =  mouseX >= x && mouseX <= x + height && mouseY >= y && mouseY <= y + height;

        boolean mouseOverX = mouseX >= this.x + s2 && mouseX <= this.x + s2 + width - s;
        mouseOver = mouseOverX && mouseY >= this.y && mouseY <= this.y + height;

        if (dragging) {
            if (mouseOverX) {
                valueWidth += mouseX - lastMouseX;
                valueWidth = Utils.clamp(valueWidth, 0, width - s);

                set((valueWidth / (width - s)) * (max - min) + min);
                if (action != null) action.run();
            } else {
                if (value > min && mouseX < this.x + s2) {
                    value = min;
                    if (action != null) action.run();
                } else if (value < max && mouseX > this.x + s2 + width - s) {
                    value = max;
                    if (action != null) action.run();
                }
            }
        }
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            if (value != valueAtDragStart && actionOnRelease != null) {
                actionOnRelease.run();
            }

            dragging = false;
            return true;
        }

        return false;
    }

    public void set(double value) {
        this.value = Utils.clamp(value, min, max);
    }

    public double get() {
        return value;
    }

    protected double valueWidth() {
        double valuePercentage = (value - min) / (max - min);
        return valuePercentage * (width - handleSize());
    }
}
