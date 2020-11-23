/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud;

import minegame159.meteorclient.utils.AlignmentX;
import minegame159.meteorclient.utils.AlignmentY;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;

public class BoundingBox implements ISerializable<BoundingBox> {
    public AlignmentX x = AlignmentX.Left;
    public AlignmentY y = AlignmentY.Top;

    public int xOffset, yOffset;
    public int width, height;

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
        xOffset += (int) Math.round(deltaX);
        yOffset += (int) Math.round(deltaY);

        double xPos = getX();
        double yPos = getY();

        // X
        switch (x) {
            case Left: {
                double c = Utils.getWindowWidth() / 3.0;

                if (xPos >= c - width / 2.0) {
                    // Module is closer to center than left
                    x = AlignmentX.Center;
                    xOffset = (int) Math.round(-c / 2.0 + xPos - c + width / 2.0);
                }

                break;
            }
            case Center: {
                double c = Utils.getWindowWidth() / 3.0;
                double cRight = Utils.getWindowWidth() / 3.0 * 2;

                if (xPos > cRight - width / 2.0) {
                    // Module is closer to right than center
                    x = AlignmentX.Right;
                    xOffset = (int) Math.round(-(c - width) + (c - (Utils.getWindowWidth() - xPos)));
                } else if (xPos < c - width / 2.0) {
                    // Module is closer to left than center
                    x = AlignmentX.Left;
                    xOffset = (int) Math.round(xPos);
                }

                break;
            }
            case Right: {
                double c = Utils.getWindowWidth() / 3.0;
                double cLeft = Utils.getWindowWidth() / 3.0 * 2;

                if (xPos <= cLeft - width / 2.0) {
                    // Module is closer to center than right
                    x = AlignmentX.Center;
                    xOffset = (int) Math.round(-c / 2.0 + xPos - c + width / 2.0);
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
                    yOffset = (int) Math.round(-c / 2.0 + yPos - c + height / 2.0);
                }

                break;
            }
            case Center: {
                double c = Utils.getWindowHeight() / 3.0;
                double cBottom = Utils.getWindowHeight() / 3.0 * 2;

                if (yPos > cBottom - height / 2.0) {
                    // Module is closer to bottom than center
                    y = AlignmentY.Bottom;
                    yOffset = (int) Math.round(-(c - height) + (c - (Utils.getWindowHeight() - yPos)));
                } else if (yPos < c - height / 2.0) {
                    // Module is closer to top than center
                    y = AlignmentY.Top;
                    yOffset = (int) Math.round(yPos);
                }

                break;
            }
            case Bottom: {
                double c = Utils.getWindowHeight() / 3.0;
                double cLeft = Utils.getWindowHeight() / 3.0 * 2;

                if (yPos <= cLeft - height / 2.0) {
                    // Module is closer to center than bottom
                    y = AlignmentY.Center;
                    yOffset = (int) Math.round(-c / 2.0 + yPos - c + height / 2.0);
                }

                break;
            }
        }

        if (y == AlignmentY.Top && yOffset < 0) yOffset = 0;
        else if (y == AlignmentY.Bottom && yOffset > 0) yOffset = 0;
    }

    public void setSize(double width, double height) {
        this.width = (int) Math.round(width);
        this.height = (int) Math.round(height);
    }

    public int getX() {
        switch (x) {
            default:     return xOffset;
            case Center: return (int) Math.round(Utils.getWindowWidth() / 2.0 - width / 2.0 + xOffset);
            case Right:  return Utils.getWindowWidth() - width + xOffset;
        }
    }

    public int getY() {
        switch (y) {
            default:     return yOffset;
            case Center: return (int) Math.round(Utils.getWindowHeight() / 2.0 - height / 2.0 + yOffset);
            case Bottom: return Utils.getWindowHeight() - height + yOffset;
        }
    }

    public boolean isOver(double x, double y) {
        int sx = getX();
        int sy = getY();

        return x >= sx && x <= sx + width && y >= sy && y <= sy + height;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("x", x.name());
        tag.putString("y", y.name());
        tag.putInt("xOffset", xOffset);
        tag.putInt("yOffset", yOffset);

        return tag;
    }

    @Override
    public BoundingBox fromTag(CompoundTag tag) {
        x = AlignmentX.valueOf(tag.getString("x"));
        y = AlignmentY.valueOf(tag.getString("y"));
        xOffset = tag.getInt("xOffset");
        yOffset = tag.getInt("yOffset");

        return this;
    }
}
