/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WRoot;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.Utils;

public abstract class WDropdown<T> extends WPressable {
    public Runnable action;

    protected T[] values;
    protected T value;

    protected double maxValueWidth;

    protected WDropdownRoot root;
    protected boolean expanded;
    protected double animProgress;

    public WDropdown(T[] values, T value) {
        this.values = values;

        set(value);
    }

    @Override
    public void init() {
        root = createRootWidget();
        root.theme = theme;
        root.spacing = 0;

        for (int i = 0; i < values.length; i++) {
            WDropdownValue widget = createValueWidget();
            widget.theme = theme;
            widget.value = values[i];

            Cell<?> cell = root.add(widget).padHorizontal(2).expandWidgetX();
            if (i >= values.length - 1) cell.padBottom(2);
        }
    }

    protected abstract WDropdownRoot createRootWidget();

    protected abstract WDropdownValue createValueWidget();

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        maxValueWidth = 0;
        for (T value : values) {
            double valueWidth = theme.textWidth(value.toString());
            maxValueWidth = Math.max(maxValueWidth, valueWidth);
        }

        root.calculateSize();

        width = pad + maxValueWidth + pad + theme.textHeight() + pad;
        height = pad + theme.textHeight() + pad;

        root.width = width;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        root.x = x;
        root.y = y + height;

        root.calculateWidgetPositions();
    }

    @Override
    protected void onPressed(int button) {
        expanded = !expanded;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    @Override
    public void move(double deltaX, double deltaY) {
        super.move(deltaX, deltaY);

        root.move(deltaX, deltaY);
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        boolean render = super.render(renderer, mouseX, mouseY, delta);

        animProgress += (expanded ? 1 : -1) * delta * 14;
        animProgress = Utils.clamp(animProgress, 0, 1);

        if (!render && animProgress > 0) {
            renderer.absolutePost(() -> {
                renderer.scissorStart(x, y + height, width, root.height * animProgress);
                root.render(renderer, mouseX, mouseY, delta);
                renderer.scissorEnd();
            });
        }

        if (expanded && root.mouseOver) theme.disableHoverColor = true;

        return render;
    }

    // Events

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (!mouseOver && !root.mouseOver) expanded = false;

        if (super.onMouseClicked(mouseX, mouseY, button, used)) used = true;
        if (expanded && root.mouseClicked(mouseX, mouseY, button, used)) used = true;

        return used;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (super.onMouseReleased(mouseX, mouseY, button)) return true;

        return expanded && root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        super.onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);

        if (expanded) root.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (super.onMouseScrolled(amount)) return true;

        if (expanded) {
            return root.mouseScrolled(amount);
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(int key, int mods) {
        if (super.onKeyPressed(key, mods)) return true;

        return expanded && root.keyPressed(key, mods);
    }

    @Override
    public boolean onKeyRepeated(int key, int mods) {
        if (super.onKeyRepeated(key, mods)) return true;

        return expanded && root.keyRepeated(key, mods);
    }

    @Override
    public boolean onCharTyped(char c) {
        if (super.onCharTyped(c)) return true;

        return expanded && root.charTyped(c);
    }

    // Widgets

    protected abstract static class WDropdownRoot extends WVerticalList implements WRoot {
        @Override
        public void invalidate() {}
    }

    protected abstract class WDropdownValue extends WPressable {
        protected T value;

        @Override
        protected void onPressed(int button) {
            boolean isNew = !WDropdown.this.value.equals(value);

            WDropdown.this.value = value;
            expanded = false;

            if (isNew && WDropdown.this.action != null) WDropdown.this.action.run();
        }
    }
}
