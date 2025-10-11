/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;

public abstract class WContainer extends WWidget {
    public final List<Cell<?>> cells = new ArrayList<>();

    public <T extends WWidget> Cell<T> add(T widget) {
        widget.parent = this;
        widget.theme = theme;

        Cell<T> cell = new Cell<>(widget).centerY();
        cells.add(cell);

        widget.init();
        invalidate();

        return cell;
    }

    public void clear() {
        if (!cells.isEmpty()) {
            cells.clear();
            invalidate();
        }
    }

    public void remove(Cell<?> cell) {
        if (cells.remove(cell)) invalidate();
    }

    @Override
    public void move(double deltaX, double deltaY) {
        super.move(deltaX, deltaY);
        for (Cell<?> cell : cells) cell.move(deltaX, deltaY);
    }

    public void moveCells(double deltaX, double deltaY) {
        for (Cell<?> cell : cells) {
            cell.move(deltaX, deltaY);

            Mouse mouse = mc.mouse;
            cell.widget().mouseMoved(mouse.getX(), mouse.getY(), mouse.getX(), mouse.getY());
        }
    }

    // Layout

    @Override
    public void calculateSize() {
        for (Cell<?> cell : cells) cell.widget().calculateSize();
        super.calculateSize();
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = 0;

        for (Cell<?> cell : cells) {
            width = Math.max(width, cell.padLeft() + cell.widget().width + cell.padRight());
            height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());
        }
    }

    @Override
    public void calculateWidgetPositions() {
        super.calculateWidgetPositions();
        for (Cell<?> cell : cells) cell.widget().calculateWidgetPositions();
    }

    @Override
    protected void onCalculateWidgetPositions() {
        for (Cell<?> cell : cells) {
            cell.x = x + cell.padLeft();
            cell.y = y + cell.padTop();

            cell.width = width - cell.padLeft() - cell.padRight();
            cell.height = height - cell.padTop() - cell.padBottom();

            cell.alignWidget();
        }
    }

    // Rendering

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (super.render(renderer, mouseX, mouseY, delta)) return true;

        for (Cell<?> cell : cells) {
            double y = cell.widget().y;
            if (y > getWindowHeight()) break;

            if (y + cell.widget().height > 0) renderWidget(cell.widget(), renderer, mouseX, mouseY, delta);
        }

        return false;
    }

    protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        widget.render(renderer, mouseX, mouseY, delta);
    }

    // Events

    protected boolean propagateEvents(WWidget widget) {
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean used) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().mouseClicked(click, used))
                    used = true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return super.mouseClicked(click, used) || used;
    }

    @Override
    public boolean mouseReleased(Click click) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().mouseReleased(click)) return true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget())) cell.widget().mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
            }
        } catch (ConcurrentModificationException ignored) {}

        super.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
    }

    @Override
    public boolean mouseScrolled(double amount) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().mouseScrolled(amount)) return true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return super.mouseScrolled(amount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().keyPressed(input)) return true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return onKeyPressed(input);
    }

    @Override
    public boolean keyRepeated(KeyInput input) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().keyRepeated(input)) return true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return onKeyRepeated(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        try {
            for (Cell<?> cell : cells) {
                if (propagateEvents(cell.widget()) && cell.widget().charTyped(input)) return true;
            }
        } catch (ConcurrentModificationException ignored) {}

        return super.charTyped(input);
    }
}
