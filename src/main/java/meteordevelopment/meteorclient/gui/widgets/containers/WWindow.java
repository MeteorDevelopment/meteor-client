/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.utils.WindowConfig;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.utils.Utils;

import java.util.function.Consumer;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public abstract class WWindow extends WVerticalList {
    public double padding = 8;
    public Consumer<WContainer> beforeHeaderInit;
    public String id;

    public final WWidget icon;
    protected final String title;

    protected WHeader header;
    public WView view;

    protected boolean dragging;
    protected boolean expanded = true;
    protected boolean dragged;

    protected double animProgress = 1;

    protected boolean moved = false;
    protected double movedX, movedY;

    private boolean propagateEventsExpanded;

    public WWindow(WWidget icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    @Override
    public void init() {
        header = header(icon);
        header.theme = theme;
        super.add(header).expandWidgetX().widget();

        view = super.add(theme.view()).expandX().pad(padding).widget();

        if (id != null) {
            expanded = theme.getWindowConfig(id).expanded;
            animProgress = expanded ? 1 : 0;
        }
    }

    protected abstract WHeader header(WWidget icon);

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return view.add(widget);
    }

    @Override
    public void clear() {
        view.clear();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;

        if (id != null) {
            WindowConfig config = theme.getWindowConfig(id);
            config.expanded = expanded;
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        if (id != null) {
            WindowConfig config = theme.getWindowConfig(id);

            if (config.x != -1) {
                x = config.x;

                if (x + width > getWindowWidth()) {
                    x = getWindowWidth() - width;
                }
            }

            if (config.y != -1) {
                y = config.y;

                if (y + height > getWindowHeight()) {
                    y = getWindowHeight() - height;
                }
            }
        }

        super.onCalculateWidgetPositions();

        if (moved) {
            move(movedX - x, movedY - y);
        }
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return true;

        boolean scissor = (animProgress != 0 && animProgress != 1) || (expanded && animProgress != 1);
        if (scissor) renderer.scissorStart(x, y, width, (height - header.height) * animProgress + header.height);
        boolean toReturn = super.render(renderer, mouseX, mouseY, delta);
        if (scissor) renderer.scissorEnd();

        return toReturn;
    }

    @Override
    protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0 || widget instanceof WHeader) {
            widget.render(renderer, mouseX, mouseY, delta);
        }

        propagateEventsExpanded = expanded;
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return widget instanceof WHeader || propagateEventsExpanded;
    }

    protected abstract class WHeader extends WContainer {
        private final WWidget icon;
        private WTriangle triangle;
        private WHorizontalList list;

        public WHeader(WWidget icon) {
            this.icon = icon;
        }

        @Override
        public void init() {
            if (icon != null) {
                createList();
                add(icon).centerY();
            }

            if (beforeHeaderInit != null) {
                createList();
                beforeHeaderInit.accept(this);
            }

            add(theme.label(title, true)).expandCellX().center().pad(4);

            triangle = add(theme.triangle()).pad(4).right().centerY().widget();
            triangle.action = () -> setExpanded(!expanded);
        }

        private void createList() {
            list = add(theme.horizontalList()).expandX().widget();
            list.spacing = 0;
        }

        @Override
        public <T extends WWidget> Cell<T> add(T widget) {
            if (list != null) return list.add(widget);
            return super.add(widget);
        }

        @Override
        protected void onCalculateSize() {
            width = 0;
            height = 0;

            for (Cell<?> cell : cells) {
                double w = cell.padLeft() + cell.widget().width + cell.padRight();
                if (cell.widget() instanceof WTriangle) w *= 2;

                width += w;
                height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());
            }
        }

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
            if (mouseOver && !used) {
                if (button == GLFW_MOUSE_BUTTON_RIGHT) setExpanded(!expanded);
                else {
                    dragging = true;
                    dragged = false;
                }

                return true;
            }

            return false;
        }

        @Override
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;

                if (!dragged) setExpanded(!expanded);
            }

            return false;
        }

        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (dragging) {
                WWindow.this.move(mouseX - lastMouseX, mouseY - lastMouseY);

                moved = true;
                movedX = x;
                movedY = y;

                if (id != null) {
                    WindowConfig config = theme.getWindowConfig(id);

                    config.x = x;
                    config.y = y;
                }

                dragged = true;
            }
        }

        @Override
        public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            animProgress += (expanded ? 1 : -1) * delta * 14;
            animProgress = Utils.clamp(animProgress, 0, 1);

            triangle.rotation = (1 - animProgress) * -90;

            return super.render(renderer, mouseX, mouseY, delta);
        }
    }
}
