/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;

import java.util.function.Consumer;

public class WSlider extends WWidget {
    private static final double HANDLE_SIZE = 15;

    public Consumer<WSlider> action;

    public double value;

    private final double min, max;
    private final double uWidth;

    private boolean handleMouseOver;
    private boolean dragging;
    private double lastMouseX;

    public WSlider(double value, double min, double max, double width) {
        this.min = min;
        this.max = max;
        this.uWidth = width;
        this.value = value;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = uWidth * GuiConfig.get().guiScale;
        height = getHandleSize();
    }

    @Override
    protected boolean onMouseClicked(boolean used, int button) {
        if (used) return false;

        if (mouseOver) {
            double valueWidth = lastMouseX - (x + getHandleSize()/2);
            value = (valueWidth / (width - getHandleSize())) * (max - min) + min;
            if (action != null) action.accept(this);

            dragging = true;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseReleased(boolean used, int button) {
        dragging = false;
        return mouseOver && !used;
    }

    @Override
    protected void onMouseMoved(double mouseX, double mouseY) {
        double valuePercentage = (value - min) / (max - min);
        double valueWidth = valuePercentage * (width - getHandleSize());

        double x = this.x + getHandleSize()/2 + valueWidth - height / 2;
        handleMouseOver =  mouseX >= x && mouseX <= x + height && mouseY >= y && mouseY <= y + height;

        boolean mouseOverX = mouseX >= this.x + getHandleSize()/2 && mouseX <= this.x + getHandleSize()/2 + width - getHandleSize();
        mouseOver = mouseOverX && mouseY >= this.y && mouseY <= this.y + height;

        if (dragging) {
            if (mouseOverX) {
                valueWidth += mouseX - lastMouseX;
                valueWidth = Utils.clamp(valueWidth, 0, width - getHandleSize());

                value = (valueWidth / (width - getHandleSize())) * (max - min) + min;
                if (action != null) action.accept(this);
            } else {
                if (value > min && mouseX < this.x + getHandleSize()/2) {
                    value = min;
                    if (action != null) action.accept(this);
                } else if (value < max && mouseX > this.x + getHandleSize()/2 + width - getHandleSize()) {
                    value = max;
                    if (action != null) action.accept(this);
                }
            }
        }

        lastMouseX = mouseX;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        value = Utils.clamp(value, min, max);
        double valuePercentage = (value - min) / (max - min);
        double valueWidth = valuePercentage * (width - getHandleSize());

        double s = GuiConfig.get().guiScale;

        renderer.quad(Region.FULL, x + getHandleSize()/2, y + 6 * s, valueWidth, 3 * s, GuiConfig.get().sliderLeft);
        renderer.quad(Region.FULL, x + getHandleSize()/2 + valueWidth, y + 6 * s, width - valueWidth - getHandleSize(), 3 * s, GuiConfig.get().sliderRight);

        Color handleColor;
        if (dragging) handleColor = GuiConfig.get().sliderHandlePressed;
        else if (handleMouseOver) handleColor = GuiConfig.get().sliderHandleHovered;
        else handleColor = GuiConfig.get().sliderHandle;

        renderer.quad(Region.CIRCLE, x + valueWidth, y, getHandleSize(), getHandleSize(), handleColor);
    }

    private double getHandleSize() {
        return HANDLE_SIZE * GuiConfig.get().guiScale;
    }
}
