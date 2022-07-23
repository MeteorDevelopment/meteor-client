/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.Utils;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public abstract class WSection extends WVerticalList {
    public Runnable action;

    protected String title;

    protected boolean expanded;
    protected double animProgress;

    private WHeader header;
    protected final WWidget headerWidget;

    private double actualWidth, actualHeight;
    private double forcedHeight = -1;
    private boolean firstTime = true;

    public WSection(String title, boolean expanded, WWidget headerWidget) {
        this.title = title;
        this.expanded = expanded;
        this.headerWidget = headerWidget;

        animProgress = expanded ? 1 : 0;
    }

    @Override
    public void init() {
        header = createHeader();
        header.theme = theme;

        super.add(header).expandX();
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return super.add(widget).padHorizontal(6);
    }

    protected abstract WHeader createHeader();

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void onCalculateSize() {
        if (forcedHeight == -1) {
            super.onCalculateSize();

            actualWidth = width;
            actualHeight = height;
        }
        else {
            width = actualWidth;
            height = forcedHeight;

            if (animProgress == 1) forcedHeight = -1;
        }

        if (firstTime) {
            firstTime = false;

            forcedHeight = (actualHeight - header.height) * animProgress + header.height;
            onCalculateSize();
        }
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return true;

        double preProgress = animProgress;

        animProgress += (expanded ? 1 : -1) * delta * 14;
        animProgress = Utils.clamp(animProgress, 0, 1);

        if (animProgress != preProgress) {
            forcedHeight = (actualHeight - header.height) * animProgress + header.height;
            invalidate();
        }

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
    }

    @Override
    protected boolean propagateEvents(WWidget widget) {
        return expanded || widget instanceof WHeader;
    }

    protected abstract class WHeader extends WHorizontalList {
        protected String title;

        public WHeader(String title) {
            this.title = title;
        }

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
            if (mouseOver && button == GLFW_MOUSE_BUTTON_LEFT && !used) {
                onClick();
                return true;
            }

            return false;
        }

        protected void onClick() {
            setExpanded(!expanded);

            if (action != null) action.run();
        }
    }
}
