/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class Cell<T extends WWidget> {
    private final T widget;

    public double x, y;
    public double width, height;

    private AlignmentX alignX = AlignmentX.Left;
    private AlignmentY alignY = AlignmentY.Top;

    private double padTop, padRight, padBottom, padLeft;
    private double marginTop;

    private boolean expandWidgetX;
    private boolean expandWidgetY;

    public boolean expandCellX;

    public Cell(T widget) {
        this.widget = widget;
    }

    public T widget() {
        return widget;
    }

    public void move(double deltaX, double deltaY) {
        x += deltaX;
        y += deltaY;

        widget.move(deltaX, deltaY);
    }

    public Cell<T> minWidth(double width) {
        widget.minWidth = width;
        return this;
    }

    // Alignment

    public Cell<T> centerX() {
        alignX = AlignmentX.Center;
        return this;
    }

    public Cell<T> right() {
        alignX = AlignmentX.Right;
        return this;
    }

    public Cell<T> centerY() {
        alignY = AlignmentY.Center;
        return this;
    }

    public Cell<T> bottom() {
        alignY = AlignmentY.Bottom;
        return this;
    }

    public Cell<T> center() {
        alignX = AlignmentX.Center;
        alignY = AlignmentY.Center;
        return this;
    }

    public Cell<T> top() {
        alignY = AlignmentY.Top;
        return this;
    }

    // Padding

    public Cell<T> padTop(double pad) {
        padTop = pad;
        return this;
    }
    public Cell<T> padRight(double pad) {
        padRight = pad;
        return this;
    }
    public Cell<T> padBottom(double pad) {
        padBottom = pad;
        return this;
    }
    public Cell<T> padLeft(double pad) {
        padLeft = pad;
        return this;
    }

    public Cell<T> padHorizontal(double pad) {
        padRight = padLeft = pad;
        return this;
    }
    public Cell<T> padVertical(double pad) {
        padTop = padBottom = pad;
        return this;
    }
    public Cell<T> pad(double pad) {
        padTop = padRight = padBottom = padLeft = pad;
        return this;
    }

    public double padTop() {
        return s(padTop);
    }
    public double padRight() {
        return s(padRight);
    }
    public double padBottom() {
        return s(padBottom);
    }
    public double padLeft() {
        return s(padLeft);
    }

    // Margin

    public Cell<T> marginTop(double m) {
        marginTop = m;
        return this;
    }

    // Expand

    public Cell<T> expandWidgetX() {
        expandWidgetX = true;
        return this;
    }

    public Cell<T> expandWidgetY() {
        expandWidgetY = true;
        return this;
    }

    public Cell<T> expandCellX() {
        expandCellX = true;
        return this;
    }

    public Cell<T> expandX() {
        expandWidgetX = true;
        expandCellX = true;
        return this;
    }

    // Other

    public void alignWidget() {
        if (expandWidgetX) {
            widget.x = x;
            widget.width = width;
        } else {
            switch (alignX) {
                case Left -> widget.x = x;
                case Center -> widget.x = x + width / 2 - widget.width / 2;
                case Right -> widget.x = x + width - widget.width;
            }
        }

        if (expandWidgetY) {
            widget.y = y;
            widget.height = height;
        } else {
            switch (alignY) {
                case Top -> widget.y = y + s(marginTop);
                case Center -> widget.y = y + height / 2 - widget.height / 2;
                case Bottom -> widget.y = y + height - widget.height;
            }
        }
    }

    private double s(double value) {
        return widget.theme.scale(value);
    }
}
