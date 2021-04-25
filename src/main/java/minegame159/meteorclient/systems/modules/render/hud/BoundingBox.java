/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud;

import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.AlignmentY;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;

public class BoundingBox implements ISerializable<BoundingBox> {
    public AlignmentX x = AlignmentX.Left;
    public AlignmentY y = AlignmentY.Top;

    public double xOffset, yOffset;
    public double width, height;

    public double alignX(double width) {
        switch (this.x) {
            default:     return 0;
            case Center: return this.width / 2.0 - width / 2.0;
            case Right:  return this.width - width;
        }
    }

    public void setX(int x) {
        addPos(x - getX(), 0);
    }

    public void setY(int y) {
        addPos(0, y - getY());
    }

    public void addPos(double deltaX, double deltaY) {
        xOffset += (deltaX);
        yOffset += (deltaY);

        double xPos = getX();
        double yPos = getY();

        // X
        switch (x) {
            case Left: {
                double c = Utils.getWindowWidth() / 3.0;

                if (xPos >= c - width / 2.0) {
                    // Module is closer to center than left
                    x = AlignmentX.Center;
                    xOffset = (-c / 2.0 + xPos - c + width / 2.0);
                }

                break;
            }
            case Center: {
                double c = Utils.getWindowWidth() / 3.0;
                double cRight = Utils.getWindowWidth() / 3.0 * 2;

                if (xPos > cRight - width / 2.0) {
                    // Module is closer to right than center
                    x = AlignmentX.Right;
                    xOffset = (-(c - width) + (c - (Utils.getWindowWidth() - xPos)));
                } else if (xPos < c - width / 2.0) {
                    // Module is closer to left than center
                    x = AlignmentX.Left;
                    xOffset = (xPos);
                }

                break;
            }
            case Right: {
                double c = Utils.getWindowWidth() / 3.0;
                double cLeft = Utils.getWindowWidth() / 3.0 * 2;

                if (xPos <= cLeft - width / 2.0) {
                    // Module is closer to center than right
                    x = AlignmentX.Center;
                    xOffset = (-c / 2.0 + xPos - c + width / 2.0);
                }

                break;
            }
        }

        if (x == AlignmentX.Left && xOffset < 0) xOffset = 0;
        else if (x == AlignmentX.Right && xOffset > 0) xOffset = 0;
        
        // Y
        switch (y) {
            case Top: {
                double c = Utils.getWindowHeight() / 3.0;

                if (yPos >= c - height / 2.0) {
                    // Module is closer to center than top
                    y = AlignmentY.Center;
                    yOffset = (-c / 2.0 + yPos - c + height / 2.0);
                }

                break;
            }
            case Center: {
                double c = Utils.getWindowHeight() / 3.0;
                double cBottom = Utils.getWindowHeight() / 3.0 * 2;

                if (yPos > cBottom - height / 2.0) {
                    // Module is closer to bottom than center
                    y = AlignmentY.Bottom;
                    yOffset = (-(c - height) + (c - (Utils.getWindowHeight() - yPos)));
                } else if (yPos < c - height / 2.0) {
                    // Module is closer to top than center
                    y = AlignmentY.Top;
                    yOffset = (yPos);
                }

                break;
            }
            case Bottom: {
                double c = Utils.getWindowHeight() / 3.0;
                double cLeft = Utils.getWindowHeight() / 3.0 * 2;

                if (yPos <= cLeft - height / 2.0) {
                    // Module is closer to center than bottom
                    y = AlignmentY.Center;
                    yOffset = (-c / 2.0 + yPos - c + height / 2.0);
                }

                break;
            }
        }

        if (y == AlignmentY.Top && yOffset < 0) yOffset = 0;
        else if (y == AlignmentY.Bottom && yOffset > 0) yOffset = 0;
    }

    public void setSize(double width, double height) {
        this.width = (width);
        this.height = (height);
    }

    public double getX() {
        switch (x) {
            default:     return xOffset;
            case Center: return (Utils.getWindowWidth() / 2.0 - width / 2.0 + xOffset);
            case Right:  return Utils.getWindowWidth() - width + xOffset;
        }
    }

    public double getY() {
        switch (y) {
            default:     return yOffset;
            case Center: return (Utils.getWindowHeight() / 2.0 - height / 2.0 + yOffset);
            case Bottom: return Utils.getWindowHeight() - height + yOffset;
        }
    }

    public boolean isOver(double x, double y) {
        double sx = getX();
        double sy = getY();

        return x >= sx && x <= sx + width && y >= sy && y <= sy + height;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("x", x.name());
        tag.putString("y", y.name());
        tag.putDouble("xOffset", xOffset);
        tag.putDouble("yOffset", yOffset);

        return tag;
    }

    @Override
    public BoundingBox fromTag(CompoundTag tag) {
        x = AlignmentX.valueOf(tag.getString("x"));
        y = AlignmentY.valueOf(tag.getString("y"));

        // It's done this way because before 0.4.2 they were stored as ints
        xOffset = ((AbstractNumberTag) tag.get("xOffset")).getDouble();
        yOffset = ((AbstractNumberTag) tag.get("yOffset")).getDouble();

        return this;
    }
}
