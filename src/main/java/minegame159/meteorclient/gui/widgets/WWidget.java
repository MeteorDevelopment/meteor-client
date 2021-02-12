/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public abstract class WWidget {
    public boolean visible = true;

    public double x, y;
    public double width, height;
    
    private double frozenWidth = -1;

    public String tooltip;

    protected WWidget parent;
    protected List<Cell<?>> cells = new ArrayList<>();

    public boolean mouseOver;
    protected double mouseOverTimer;

    public void invalidate() {
        WWidget root = getRoot();
        if (root != null) root.invalidate();
    }

    public <T extends WWidget> Cell<T> add(T widget) {
        widget.parent = this;
        Cell<T> cell = new Cell<>();
        cell.widget = widget;
        cells.add(cell);
        invalidate();
        return cell;
    }

    public <T extends WWidget> void remove(T widget) {
        Cell<T> temp = new Cell<>();
        temp.widget = widget;
        if (cells.remove(temp)) invalidate();
    }

    public void clear() {
        if (cells.size() > 0) {
            cells.clear();
            invalidate();
        }
    }

    public void move(double deltaX, double deltaY, boolean callMouseMoved) {
        move(this, deltaX, deltaY, callMouseMoved);
        if (callMouseMoved) mouseMoved(MinecraftClient.getInstance().mouse.getX(), MinecraftClient.getInstance().mouse.getY());
    }
    protected void move(WWidget widget, double deltaX, double deltaY, boolean callMouseMoved) {
        widget.x += deltaX;
        widget.y += deltaY;
        widget.onMoved(deltaX, deltaY, callMouseMoved);

        for (Cell<?> cell : widget.cells) {
            cell.x += deltaX;
            cell.y += deltaY;
            move(cell.getWidget(), deltaX, deltaY, callMouseMoved);
        }
    }

    protected void onMoved(double deltaX, double deltaY, boolean callMouseMoved) {}

    public void freezeWidth() {
        frozenWidth = width;
    }

    protected void calculateSize(GuiRenderer renderer) {
        for (Cell cell : cells) cell.widget.calculateSize(renderer);
        onCalculateSize(renderer);

        if (frozenWidth != -1) {
            width = frozenWidth;
            frozenWidth = -1;
        }

        width = Math.round(width);
        height = Math.round(height);
    }
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 0;
        height = 0;

        for (Cell<?> cell : cells) {
            width = Math.max(width, cell.padLeft + cell.getWidget().width + cell.padRight);
            height = Math.max(height, cell.padTop + cell.getWidget().height + cell.padBottom);
        }
    }

    protected void calculateWidgetPositions() {
        x = Math.round(x);
        y = Math.round(y);

        onCalculateWidgetPositions();
        for (Cell cell : cells) cell.widget.calculateWidgetPositions();
    }
    protected void onCalculateWidgetPositions() {
        for (Cell<?> cell : cells) {
            cell.x = x + cell.padLeft;
            cell.y = y + cell.padTop;
            cell.width = width - cell.padLeft - cell.padRight;
            cell.height = height - cell.padTop - cell.padBottom;
            cell.alignWidget();
        }
    }

    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;
        if (mouseOver) mouseOverTimer += delta / 20.0;
        onRender(renderer, mouseX, mouseY, delta);
        for (Cell<?> cell : cells) {
            if (cell.widget.y > MinecraftClient.getInstance().getWindow().getFramebufferHeight()) break;
            onRenderWidget(cell.getWidget(), renderer, mouseX, mouseY, delta);
        }
        if (mouseOver && mouseOverTimer >= 1 && tooltip != null) renderer.tooltip = tooltip;
    }
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        widget.render(renderer, mouseX, mouseY, delta);
    }
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {}

    public WWidget getRoot() {
        return parent != null ? parent.getRoot() : (this instanceof WRoot ? this : null);
    }

    public List<Cell<?>> getCells() {
        return cells;
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    // Events

    protected boolean propagateEvents(WWidget widget) {
        return !(parent instanceof WView) || parent.propagateEvents(widget);
    }

    public void mouseMoved(double mouseX, double mouseY) {
        for (Cell<?> cell : cells) {
            if (propagateEvents(cell.getWidget())) {
                cell.getWidget().mouseMoved(mouseX, mouseY);
            }
        }
        boolean preMouseOver = mouseOver;
        mouseOver = isOver(mouseX, mouseY);
        if (!preMouseOver && mouseOver) mouseOverTimer = 0;
        onMouseMoved(mouseX, mouseY);
    }
    protected void onMouseMoved(double mouseX, double mouseY) {}

    public boolean mouseClicked(boolean used, int button) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.getWidget())) {
                    if (cell.getWidget().mouseClicked(used, button)) used = true;
                }
            }
        } catch (ConcurrentModificationException ignored) {}
        return onMouseClicked(used, button);
    }
    protected boolean onMouseClicked(boolean used, int button) { return used; }

    public boolean mouseReleased(boolean used, int button) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.getWidget())) {
                    if (cell.getWidget().mouseReleased(used, button)) used = true;
                }
            }
        } catch (ConcurrentModificationException ignored) {}
        return onMouseReleased(used, button);
    }
    protected boolean onMouseReleased(boolean used, int button) { return used; }

    public boolean mouseScrolled(double amount) {
        for (Cell<?> cell : cells) {
            if (propagateEvents(cell.getWidget())) {
                if (cell.getWidget().mouseScrolled(amount)) return true;
            }
        }
        return onMouseScrolled(amount);
    }
    protected boolean onMouseScrolled(double amount) { return false; }

    public boolean keyPressed(int key, int mods) {
        for (Cell<?> cell : cells) {
            if (propagateEvents(cell.getWidget())) {
                if (cell.getWidget().keyPressed(key, mods)) return true;
            }
        }
        return onKeyPressed(key, mods);
    }
    protected boolean onKeyPressed(int key, int mods) { return false; }

    public boolean keyRepeated(int key, int mods) {
        for (Cell<?> cell : cells) {
            if (propagateEvents(cell.getWidget())) {
                if (cell.getWidget().keyRepeated(key, mods)) return true;
            }
        }
        return onKeyRepeated(key, mods);
    }
    protected boolean onKeyRepeated(int key, int mods) { return false; }

    public boolean charTyped(char c, int key) {
        for (Cell<?> cell : cells) {
            if (propagateEvents(cell.getWidget())) {
                if (cell.getWidget().charTyped(c, key)) return true;
            }
        }
        return onCharTyped(c, key);
    }
    protected boolean onCharTyped(char c, int key) { return false; }
}
