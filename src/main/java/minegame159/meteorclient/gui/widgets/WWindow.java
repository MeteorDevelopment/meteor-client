/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.Utils;
import org.lwjgl.glfw.GLFW;

public class WWindow extends WTable {
    public Runnable action;
    public GuiConfig.WindowType type;

    private final WHeader header;
    private final WTable table;

    private boolean wasMoved;
    private double mX, mY;

    private boolean expanded;
    private double animationProgress;

    public WWindow(String title, boolean expanded, boolean scrollOnlyWhenMouseOver) {
        this.expanded = expanded;
        this.animationProgress = expanded ? 1 : 0;

        defaultCell.space(0);

        header = super.add(new WHeader(title)).fillX().expandX().getWidget();
        super.row();

        table = super.add(new WView(scrollOnlyWhenMouseOver)).fillX().expandX().getWidget().add(new WTable()).fillX().expandX().getWidget();
        table.pad(8);
    }

    public WWindow(String title, boolean expanded) {
        this(title, expanded, false);
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return table.add(widget);
    }

    @Override
    public void row() {
        table.row();
    }

    @Override
    public <T extends WWidget> void remove(T widget) {
        table.remove(widget);
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public WTable pad(double pad) {
        return table.pad(pad);
    }

    public Cell<?> getDefaultCell() {
        return table.defaultCell;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        if (wasMoved) {
            move(mX - x, mY - y, false);
        }
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;

        boolean scissor = (animationProgress != 0 && animationProgress != 1) || (expanded && animationProgress != 1);
        if (scissor) renderer.beginScissor(x, y, width, (height - header.height) * animationProgress + header.height);
        super.render(renderer, mouseX, mouseY, delta);
        if (scissor) renderer.endScissor();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animationProgress > 0) {
            renderer.quad(Region.FULL, x, y + header.height, width, height - header.height, GuiConfig.INSTANCE.background);
        }
    }

    @Override
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animationProgress > 0 || widget instanceof WHeader) {
            widget.render(renderer, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return expanded || widget instanceof WHeader;
    }

    private class WHeader extends WWidget {
        private final String title;
        private final WTriangle triangle;

        private boolean dragging;
        private double lastMouseX, lastMouseY;

        public WHeader(String title) {
            this.title = title;

            add(new WTitle(title)).pad(4).fillX().centerX();

            triangle = add(new WTriangle()).pad(4).fillX().centerY().right().getWidget();
            triangle.action = () -> {
                expanded = !expanded;
                if (type != null) GuiConfig.INSTANCE.getWindowConfig(type).setExpanded(expanded);
            };
        }

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = 4 + renderer.textWidth(title) + 4 + 4 + renderer.textHeight() + 4 + 44;
            height = 0;

            for (Cell<?> cell : cells) {
                height = Math.max(height, cell.padTop + cell.getWidget().height + cell.padBottom);
            }
        }

        @Override
        protected boolean onMouseClicked(boolean used, int button) {
            if (used) return false;

            if (mouseOver) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    expanded = !expanded;
                    if (type != null) GuiConfig.INSTANCE.getWindowConfig(type).setExpanded(expanded);
                    return true;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    dragging = true;
                    return true;
                }
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
            if (dragging) {
                WWindow.this.move(mouseX - lastMouseX, mouseY - lastMouseY, false);

                wasMoved = true;
                mX = WWindow.this.x;
                mY = WWindow.this.y;

                if (action != null) action.run();
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.quad(Region.FULL, x, y, width, height, GuiConfig.INSTANCE.accent);

            if (expanded) animationProgress += delta / 4;
            else animationProgress -= delta / 4;
            animationProgress = Utils.clamp(animationProgress, 0, 1);

            triangle.rotation = (1 - animationProgress) * -90;
        }
    }
}
