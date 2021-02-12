/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class WView extends WTable {
    public double maxHeight;

    private boolean hasScrollBar;
    private double actualHeight;

    private final boolean onlyWhenMouseOver;
    private double scrollHeight, lastScrollHeight;

    private boolean moveWidgetsOnCalculatePositions;

    public WView(boolean onlyWhenMouseOver) {
        this.onlyWhenMouseOver = onlyWhenMouseOver;

        maxHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight() - 128;
        pad(0);
    }

    public WView() {
        this(false);
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        super.onCalculateSize(renderer);

        recalculateScroll();
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        if (moveWidgetsOnCalculatePositions) {
            moveWidgets();
            moveWidgetsOnCalculatePositions = false;
        }
    }

    private void recalculateScroll() {
        boolean hadScrollBar = hasScrollBar;

        if (height > maxHeight) {
            hasScrollBar = true;
            actualHeight = height;
            height = maxHeight;

            if (hadScrollBar) {
                lastScrollHeight = 0;
                moveWidgetsOnCalculatePositions = true;
            } else {
                scrollHeight = 0;
                lastScrollHeight = 0;
            }
        } else {
            if (hadScrollBar) {
                lastScrollHeight = 0;
                moveWidgetsOnCalculatePositions = true;
            }
            hasScrollBar = false;
            actualHeight = height;
        }
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (hasScrollBar && (!onlyWhenMouseOver || mouseOver)) {
            scrollHeight -= amount * 22 * GuiConfig.get().scrollSensitivity;
            moveWidgets();
            return true;
        }

        return false;
    }

    public double changeHeight(double delta) {
        double preHeight = height;
        height = actualHeight + delta;
        recalculateScroll();
        return preHeight - height;
    }

    public void moveWidgets() {
        scrollHeight = Utils.clamp(scrollHeight, 0, actualHeight - height);

        double deltaY = -(scrollHeight - lastScrollHeight);
        lastScrollHeight = scrollHeight;

        moveWidgets(deltaY);
    }

    private void moveWidgets(double deltaY) {
        for (Cell<?> cell : getCells()) move(cell.getWidget(), 0, deltaY, false);
        mouseMoved(MinecraftClient.getInstance().mouse.getX(), MinecraftClient.getInstance().mouse.getY());
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;

        boolean scissor = hasScrollBar;
        if (scissor) renderer.beginScissor(x, y, width, height);
        super.render(renderer, mouseX, mouseY, delta);
        if (scissor) renderer.endScissor();
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return ((widget.y >= y && widget.y <= y + height) || (widget.y + widget.height >= y && widget.y + widget.height <= y + height)) ||
                ((y >= widget.y && y <= widget.y + widget.height) || (y + height >= widget.y && y + height <= widget.y + widget.height));
    }
}
