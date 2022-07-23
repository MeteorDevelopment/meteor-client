/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

public class HudBox implements ISerializable<HudBox> {
    private final HudElement element;

    public XAnchor xAnchor = XAnchor.Left;
    public YAnchor yAnchor = YAnchor.Top;

    public int x, y;
    int width, height;

    public HudBox(HudElement element) {
        this.element = element;
    }

    public void setSize(double width, double height) {
        if (width >= 0) this.width = (int) Math.ceil(width);
        if (height >= 0) this.height = (int) Math.ceil(height);
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setXAnchor(XAnchor anchor) {
        if (xAnchor != anchor) {
            int renderX = getRenderX();

            switch (anchor) {
                case Left -> x = renderX;
                case Center -> x = renderX + width / 2 - Utils.getWindowWidth() / 2;
                case Right -> x = renderX + width - Utils.getWindowWidth();
            }

            xAnchor = anchor;
        }
    }

    public void setYAnchor(YAnchor anchor) {
        if (yAnchor != anchor) {
            int renderY = getRenderY();

            switch (anchor) {
                case Top -> y = renderY;
                case Center -> y = renderY + height / 2 - Utils.getWindowHeight() / 2;
                case Bottom -> y = renderY + height - Utils.getWindowHeight();
            }

            yAnchor = anchor;
        }
    }

    public void updateAnchors() {
        setXAnchor(getXAnchor(getRenderX()));
        setYAnchor(getYAnchor(getRenderY()));
    }

    public void move(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;

        if (element.autoAnchors) updateAnchors();

        int border = Hud.get().border.get();

        // Clamp X
        if (xAnchor == XAnchor.Left && x < border) x = border;
        else if (xAnchor == XAnchor.Right && x > border) x = border;

        // Clamp Y
        if (yAnchor == YAnchor.Top && y < border) y = border;
        else if (yAnchor == YAnchor.Bottom && y > border) y = border;
    }

    public XAnchor getXAnchor(double x) {
        double splitLeft = Utils.getWindowWidth() / 3.0;
        double splitRight = splitLeft * 2;

        boolean left = x <= splitLeft;
        boolean right = x + width >= splitRight;

        if ((left && right) || (!left && !right)) return XAnchor.Center;
        return left ? XAnchor.Left : XAnchor.Right;
    }

    public YAnchor getYAnchor(double y) {
        double splitTop = Utils.getWindowHeight() / 3.0;
        double splitBottom = splitTop * 2;

        boolean top = y <= splitTop;
        boolean bottom = y + height >= splitBottom;

        if ((top && bottom) || (!top && !bottom)) return YAnchor.Center;
        return top ? YAnchor.Top : YAnchor.Bottom;
    }

    public int getRenderX() {
        return switch (xAnchor) {
            case Left -> x;
            case Center -> Utils.getWindowWidth() / 2 - width / 2 + x;
            case Right -> Utils.getWindowWidth() - width + x;
        };
    }

    public int getRenderY() {
        return switch (yAnchor) {
            case Top -> y;
            case Center -> Utils.getWindowHeight() / 2 - height / 2 + y;
            case Bottom -> Utils.getWindowHeight() - height + y;
        };
    }

    public double alignX(double selfWidth, double width, Alignment alignment) {
        XAnchor anchor = xAnchor;

        if (alignment == Alignment.Left) anchor = XAnchor.Left;
        else if (alignment == Alignment.Center) anchor = XAnchor.Center;
        else if (alignment == Alignment.Right) anchor = XAnchor.Right;

        return switch (anchor) {
            case Left -> 0;
            case Center -> selfWidth / 2.0 - width / 2.0;
            case Right -> selfWidth - width;
        };
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("x-anchor", xAnchor.name());
        tag.putString("y-anchor", yAnchor.name());
        tag.putInt("x", x);
        tag.putInt("y", y);

        return tag;
    }

    @Override
    public HudBox fromTag(NbtCompound tag) {
        if (tag.contains("x-anchor")) xAnchor = XAnchor.valueOf(tag.getString("x-anchor"));
        if (tag.contains("y-anchor")) yAnchor = YAnchor.valueOf(tag.getString("y-anchor"));
        if (tag.contains("x")) x = tag.getInt("x");
        if (tag.contains("y")) y = tag.getInt("y");

        return this;
    }
}
