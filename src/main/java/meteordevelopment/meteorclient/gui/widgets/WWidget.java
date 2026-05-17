/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WWidget implements BaseWidget {
    public boolean visible = true;
    public GuiTheme theme;

    public double x, y;
    public double width, height;
    public double minWidth;

    public WWidget parent;
    public String tooltip;

    public boolean mouseOver;
    public boolean focused;
    protected boolean instantTooltips;
    protected double mouseOverTimer;

    public void init() {
    }

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

            if ((instantTooltips || mouseOverTimer >= 1) && tooltip != null) {
                WView view = getView();
                if (view == null || view.mouseOver) renderer.tooltip(tooltip);
            }
        } else {
            mouseOverTimer = 0;
        }

        onRender(renderer, mouseX, mouseY, delta);
        return false;
    }

    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    }

    // Events

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return onMouseClicked(click, doubled);
    }

    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent click) {
        return onMouseReleased(click);
    }

    public boolean onMouseReleased(MouseButtonEvent click) {
        return false;
    }

    public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        mouseOver = isOver(mouseX, mouseY);
        onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
    }

    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
    }

    public boolean mouseScrolled(double amount) {
        return onMouseScrolled(amount);
    }

    public boolean onMouseScrolled(double amount) {
        return false;
    }

    public boolean keyPressed(KeyEvent input) {
        return onKeyPressed(input);
    }

    public boolean onKeyPressed(KeyEvent input) {
        return false;
    }

    public boolean keyRepeated(KeyEvent input) {
        return onKeyRepeated(input);
    }

    public boolean onKeyRepeated(KeyEvent input) {
        return false;
    }

    public boolean charTyped(CharacterEvent input) {
        return onCharTyped(input);
    }

    public boolean onCharTyped(CharacterEvent input) {
        return false;
    }

    // Other

    public void invalidate() {
        WWidget root = getRoot();
        if (root != null) root.invalidate();
    }

    protected WWidget getRoot() {
        return parent != null ? parent.getRoot() : (this instanceof WRoot ? this : null);
    }

    public WView getView() {
        if (this instanceof WView view) return view;
        return parent != null ? parent.getView() : null;
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (this.focused != focused) this.focused = focused;
    }
}
