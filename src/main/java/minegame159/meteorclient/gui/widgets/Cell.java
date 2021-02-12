/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.AlignmentY;

public class Cell<T extends WWidget> {
    T widget;

    public double x, y;
    public double width, height;

    double padTop, padRight, padBottom, padLeft;
    double spaceTop, spaceRight, spaceBottom, spaceLeft;

    private AlignmentX alignX = AlignmentX.Left;
    private AlignmentY alignY = AlignmentY.Top;

    boolean fillX;
    private boolean expandX, expandY;

    public T getWidget() {
        return widget;
    }

    // Spacing

    public Cell<T> spaceTop(double space) {
        spaceTop = space;
        return this;
    }
    public Cell<T> spaceRight(double space) {
        spaceRight = space;
        return this;
    }
    public Cell<T> spaceBottom(double space) {
        spaceBottom = space;
        return this;
    }
    public Cell<T> spaceLeft(double space) {
        spaceLeft = space;
        return this;
    }

    public Cell<T> spaceHorizontal(double space) {
        spaceRight = space;
        spaceLeft = space;
        return this;
    }
    public Cell<T> spaceVertical(double space) {
        spaceTop = space;
        spaceBottom = space;
        return this;
    }
    public Cell<T> space(double space) {
        spaceTop = spaceRight = spaceBottom = spaceLeft = space;
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

    // Expand

    public Cell<T> expandX() {
        expandX = true;
        return this;
    }
    public Cell<T> expandY() {
        expandY = true;
        return this;
    }

    // Fill

    public Cell<T> fillX() {
        fillX = true;
        return this;
    }

    // Alignment

    public Cell<T> right() {
        alignX = AlignmentX.Right;
        return this;
    }
    public Cell<T> centerX() {
        alignX = AlignmentX.Center;
        return this;
    }
    public Cell<T> left() {
        alignX = AlignmentX.Left;
        return this;
    }

    public Cell<T> top() {
        alignY = AlignmentY.Top;
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

    public Cell<T> centerXY() {
        alignX = AlignmentX.Center;
        alignY = AlignmentY.Center;
        return this;
    }

    public void set(Cell<?> cell) {
        x = cell.x;
        y = cell.y;
        width = cell.width;
        height = cell.height;
        padTop = cell.padTop;
        padRight = cell.padRight;
        padBottom = cell.padBottom;
        padLeft = cell.padLeft;
        spaceTop = cell.spaceTop;
        spaceRight = cell.spaceRight;
        spaceBottom = cell.spaceBottom;
        spaceLeft = cell.spaceLeft;
        alignX = cell.alignX;
        alignY = cell.alignY;
        fillX = cell.fillX;
        expandX = expandY = cell.expandY;
    }

    public void alignWidget() {
        if (expandX) {
            widget.x = x;
            widget.width = width;
        } else {
            switch (alignX) {
                case Left:   widget.x = x; break;
                case Center: widget.x = x + width / 2 - widget.width / 2; break;
                case Right:  widget.x = x + width - widget.width; break;
            }
        }

        if (expandY) {
            widget.y = y;
            widget.height = height;
        } else {
            switch (alignY) {
                case Top:    widget.y = y; break;
                case Center: widget.y = y + height / 2 - widget.height / 2; break;
                case Bottom: widget.y = y + height - widget.height; break;
            }
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
