/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;

public abstract class WWidget implements BaseWidget {
    public boolean visible = true;
    public GuiTheme theme;

    public double x, y;
    public double width, height;
    public double minWidth;

    public WWidget parent;
    public String tooltip;

    public boolean mouseOver;
    protected double mouseOverTimer;

    public void init() {}

    public void move(double deltaX, double deltaY) {
        x = Math.round(x + deltaX);
        y = Math.round(y + deltaY);
    }

    @Override
    public GuiTheme getTheme() {
        return theme;
    }

    public double pad() {
        return theme.pad();
    }

    // Layout

    public void calculateSize() {
        onCalculateSize();

        double minWidth = theme.scale(this.minWidth);
        if (width < minWidth) width = minWidth;

        width = Math.round(width);
        height = Math.round(height);
    }

    protected void onCalculateSize() {

    }

    public void calculateWidgetPositions() {
        x = Math.round(x);
        y = Math.round(y);

        onCalculateWidgetPositions();
    }

    protected void onCalculateWidgetPositions() {

    }

    // Rendering

    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return true;

        if (isOver(mouseX, mouseY)) {
            mouseOverTimer += delta;
            if (mouseOverTimer >= 1 && tooltip != null) renderer.tooltip(tooltip);
        }
        else {
            mouseOverTimer = 0;
        }

        onRender(renderer, mouseX, mouseY, delta);
        return false;
    }

    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {}

    // Events

    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean used) {
        return onMouseClicked(mouseX, mouseY, button, used);
    }
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) { return false; }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return onMouseReleased(mouseX, mouseY, button);
    }
    public boolean onMouseReleased(double mouseX, double mouseY, int button) { return false; }

    public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        mouseOver = isOver(mouseX, mouseY);
        onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
    }
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {}

    public boolean mouseScrolled(double amount) {
        return onMouseScrolled(amount);
    }
    public boolean onMouseScrolled(double amount) { return false; }

    public boolean keyPressed(int key, int mods) {
        return onKeyPressed(key, mods);
    }
    public boolean onKeyPressed(int key, int mods) { return false; }

    public boolean keyRepeated(int key, int mods) {
        return onKeyRepeated(key, mods);
    }
    public boolean onKeyRepeated(int key, int mods) { return false; }

    public boolean charTyped(char c) {
        return onCharTyped(c);
    }
    public boolean onCharTyped(char c) { return false; }

    // Other

    public void invalidate() {
        WWidget root = getRoot();
        if (root != null) root.invalidate();
    }

    protected WWidget getRoot() {
        return parent != null ? parent.getRoot() : (this instanceof WRoot ? this : null);
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }
}
