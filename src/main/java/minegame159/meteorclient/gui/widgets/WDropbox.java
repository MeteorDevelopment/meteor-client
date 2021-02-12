/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.misc.CursorStyle;
import minegame159.meteorclient.utils.render.color.Color;

import java.lang.reflect.InvocationTargetException;

public class WDropbox<T extends Enum<?>> extends WWidget {
    private static final GuiRenderer RENDERER = new GuiRenderer();

    public Runnable action;

    private T value;
    private String valueName;
    private double valueNameWidth;

    private WTableRoot root;
    private boolean open;

    public WDropbox(T value) {
        try {
            Class<?> klass = value.getClass();
            T[] values = (T[]) klass.getDeclaredMethod("values").invoke(null);

            root = new WTableRoot();
            root.pad(0);
            root.defaultCell.space(0);
            for (T v : values) {
                root.add(new WOption(v)).expandX();
                root.row();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        setValue(value);
    }

    public void setValue(T value) {
        this.value = value;
        this.valueName = value.toString();
        this.valueNameWidth = -1;
    }

    public T getValue() {
        return value;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        root.calculateSize(RENDERER);

        double s = GuiConfig.get().guiScale;
        valueNameWidth = renderer.textWidth(valueName);
        width = 6 * s + root.width + 4 * s + renderer.textHeight() + 6 * s;
        height = 6 * s + renderer.textHeight() + 6 * s;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        root.x = x;
        root.y = y + height;
        root.calculateWidgetPositions();
    }

    @Override
    protected boolean onMouseClicked(boolean used, int button) {
        if (open) {
            if (root.mouseClicked(used, button)) return true;
        }

        if (open && (!mouseOver && !root.mouseOver)) {
            open = false;
            return true;
        }

        if (used) return open;

        if (mouseOver) {
            open = !open;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseReleased(boolean used, int button) {
        if (open) root.mouseReleased(used, button);
        return open;
    }

    @Override
    protected void onMouseMoved(double mouseX, double mouseY) {
        if (open) root.mouseMoved(mouseX, mouseY);
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (open) root.mouseScrolled(amount);
        return open;
    }

    @Override
    protected boolean onKeyPressed(int key, int mods) {
        if (open) root.keyPressed(key, mods);
        return open;
    }

    @Override
    protected boolean onKeyRepeated(int key, int mods) {
        if (open) root.keyRepeated(key, mods);
        return open;
    }

    @Override
    protected boolean onCharTyped(char c, int key) {
        if (open) root.charTyped(c, key);
        return open;
    }

    @Override
    protected void onMoved(double deltaX, double deltaY, boolean callMouseMoved) {
        root.move(deltaX, deltaY, callMouseMoved);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (mouseOver) renderer.setCursorStyle(CursorStyle.Click);

        renderer.background(this, mouseOver, false);

        if (valueNameWidth == -1) valueNameWidth = renderer.textWidth(valueName);

        renderer.text(valueName, x + 6 + (root.width - valueNameWidth) / 2, y + 6, false, GuiConfig.get().text);
        renderer.triangle(x + 6 + root.width + 4, y + 6 + renderer.textHeight() / 4, renderer.textHeight(), 0, GuiConfig.get().separator);

        if (open) {
            renderer.post(() -> {
                RENDERER.begin();
                root.render(RENDERER, mouseX, mouseY, delta);
                RENDERER.end();
            });
        }
    }

    private class WOption extends WWidget {
        private final T value;
        private final String text;
        private double a;

        public WOption(T value) {
            this.value = value;
            this.text = value.toString();
        }

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            a = renderer.textHeight() + 6 + 4 + 6;
            width = 6 + renderer.textWidth(text) + 6;
            height = 6 + renderer.textHeight() + 6;
        }

        @Override
        public boolean isOver(double x, double y) {
            return x >= this.x && x <= this.x + width + a && y >= this.y && y <= this.y + height;
        }

        @Override
        protected boolean onMouseClicked(boolean used, int button) {
            if (used) return false;

            if (mouseOver) {
                open = false;
                setValue(value);
                if (action != null) action.run();
                return true;
            }

            return false;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (mouseOver) renderer.setCursorStyle(CursorStyle.Click);

            Color color = GuiConfig.get().background;
            if (mouseOver || WDropbox.this.value == value) color = GuiConfig.get().backgroundHovered;

            int preAlpha = color.a;
            color.a = 255;
            renderer.quad(Region.FULL, x + 1, y, width + a - 2, height, color);
            color.a = preAlpha;

            renderer.text(text, x + 6 + 1, y + 6 + 1, false, GuiConfig.get().text);
        }
    }

    private static class WTableRoot extends WTable implements WRoot {
        @Override
        public void invalidate() {}

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Color color = GuiConfig.get().outline;
            int preAlpha = color.a;
            color.a = 255;

            double a = 6 + renderer.textHeight() + 4 + 6;

            renderer.quad(Region.FULL, x, y, 1, height, color);
            renderer.quad(Region.FULL, x + width + a - 1, y, 1, height, color);
            renderer.quad(Region.FULL, x, y + height, width + a, 1, color);

            color.a = preAlpha;
        }
    }
}
