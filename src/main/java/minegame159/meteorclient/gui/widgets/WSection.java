/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.CursorStyle;

import java.util.List;

public class WSection extends WTable {
    public Runnable action;

    private final WHeader header;
    private final WTable table;

    private boolean expanded;
    private double animationProgress;
    private boolean idkDudeNamingIsHard;

    public WSection(String name, boolean expanded, WWidget headerWidget) {
        defaultCell.space(0);

        header = super.add(new WHeader(name, headerWidget)).fillX().expandX().getWidget();
        super.row();
        table = super.add(new WTable()).fillX().expandX().getWidget();
        table.padHorizontal(12);

        this.expanded = expanded;
        this.animationProgress = expanded ? 1 : 0;
        if (!expanded) idkDudeNamingIsHard = true;
    }

    public WSection(String name, boolean expanded) {
        this(name, expanded, null);
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
    public List<Cell<?>> getCells() {
        return table.getCells();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded, boolean animation) {
        this.expanded = expanded;
        if (!animation) animationProgress = expanded ? 1 : 0;
        if (action != null) action.run();
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
        double preAnimationProgress = animationProgress;

        if (idkDudeNamingIsHard) {
            preAnimationProgress = 1;
            idkDudeNamingIsHard = false;
        }

        if (expanded) animationProgress += delta / 4;
        else animationProgress -= delta / 4;
        animationProgress = Utils.clamp(animationProgress, 0, 1);

        header.triangle.rotation = (1 - animationProgress) * -90;

        if (animationProgress != preAnimationProgress) {
            double deltaAnimationProgress = animationProgress - preAnimationProgress;
            double toMove = deltaAnimationProgress * (height - header.height);

            // Move widgets under this section in parents
            moveParent(this, toMove);

            // Change height of parents
            changeHeight(parent, toMove);
        }
    }

    private void moveParent(WWidget widget, double toMove) {
        int myI = getCellIndexInParent(widget);

        if (myI < widget.parent.getCells().size()) {
            for (int i = myI + 1; i < widget.parent.getCells().size(); i++) {
                Cell<?> cell = widget.parent.getCells().get(i);

                cell.y = Math.round(cell.y + toMove);
                cell.getWidget().move(0, toMove, false);
            }
        }

        if (widget.parent.parent != null && !(widget.parent.parent instanceof WWindow)) moveParent(widget.parent, toMove);
    }

    private int getCellIndexInParent(WWidget widget) {
        for (int i = 0; i < widget.parent.getCells().size(); i++) {
            if (widget.parent.getCells().get(i).getWidget() == widget) return i;
        }

        return -1;
    }

    private void changeHeight(WWidget widget, double delta) {
        if (widget instanceof WView) {
            double change = ((WView) widget).changeHeight(delta);
            if (change != 0) delta = -change;
            else return;
        } else {
            widget.height += delta;
        }

        if (widget.parent != null && !(widget.parent instanceof WRoot)) changeHeight(widget.parent, delta);
    }

    @Override
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || (animationProgress > 0 && animationProgress < 1) || widget instanceof WHeader) {
            widget.render(renderer, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return expanded || widget instanceof WHeader;
    }

    private class WHeader extends WTable {
        final WTriangle triangle;

        WHeader(String name, WWidget widget) {
            add(new WHorizontalSeparator(name));
            removeRow();
            if (widget != null) add(widget);
            triangle = add(new WTriangle()).pad(4).getWidget();
            triangle.color = triangle.colorHovered = triangle.colorPressed = GuiConfig.get().separator;

            triangle.action = () -> {
                expanded = !expanded;
                if (action != null) action.run();
            };
        }

        @Override
        protected boolean onMouseClicked(boolean used, int button) {
            if (used) return false;

            if (mouseOver) {
                expanded = !expanded;
                if (action != null) action.run();
            }

            return mouseOver;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (mouseOver) renderer.setCursorStyle(CursorStyle.Click);

            super.onRender(renderer, mouseX, mouseY, delta);
        }
    }
}
