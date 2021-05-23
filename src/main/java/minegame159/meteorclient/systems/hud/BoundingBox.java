/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud;

import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.render.AnchorX;
import minegame159.meteorclient.utils.render.AnchorY;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;

public class BoundingBox implements ISerializable<BoundingBox> {
    public AnchorX xAnchor = AnchorX.Left;
    public double xOffset;
    public double width;

    public AnchorY yAnchor = AnchorY.Top;
    public double yOffset;
    public double height;

    public AnchorX boxAnchorX = AnchorX.Left;
    public AnchorY boxAnchorY = AnchorY.Top;

    public double getX() {
        switch (xAnchor) {
            default:     return xOffset;
            case Center: return Utils.getWindowWidth() / 2.0 + xOffset;
            case Right:  return Utils.getWindowWidth() + xOffset;
        }
    }

    public void setX(double x) {
        setPos(x, getY());
    }

    public double getY() {
        switch (yAnchor) {
            default:     return yOffset;
            case Center: return Utils.getWindowHeight() / 2.0 + yOffset;
            case Bottom: return Utils.getWindowHeight() + yOffset;
        }
    }

    public void setY(double y) {
        setPos(getX(), y);
    }

    public void setPos(double x, double y) {
        addPos(x - getX(), y - getY());
    }

    public void addPos(double deltaX, double deltaY) {
        xOffset += deltaX;
        yOffset += deltaY;

        double xPos = getX();
        double xThird = Utils.getWindowWidth() / 3.0;

        double yPos = getY();
        double yThird = Utils.getWindowHeight() / 3.0;

        double margin = HUD.get().margin.get() * MinecraftClient.getInstance().getWindow().getScaleFactor();
        double snapRange = HUD.get().snappingRange.get() * MinecraftClient.getInstance().getWindow().getScaleFactor();

        double xCheck;
        switch (boxAnchorX) {
            default:     xCheck = xPos; break;
            case Center: xCheck = xPos + (width / 2.0); break;
            case Right:  xCheck = xPos + width; break;
        }

        // X
        switch (xAnchor) {
            case Left: {
                // Center
                if (xCheck > xThird) {
                    xAnchor = AnchorX.Center;
                    xOffset = xPos - (Utils.getWindowWidth() / 2.0);
                }

                break;
            }
            case Center: {
                // Left
                if (xCheck <= xThird) {
                    xAnchor = AnchorX.Left;
                    xOffset = xPos;
                }

                // Right
                else if (xCheck >= xThird * 2) {
                    xAnchor = AnchorX.Right;
                    xOffset = xPos - Utils.getWindowWidth();
                }

                break;
            }
            case Right: {
                // Center
                if (xCheck < xThird * 2) {
                    xAnchor = AnchorX.Center;
                    xOffset = xPos - (Utils.getWindowWidth() / 2.0);
                }

                break;
            }
        }

        if (xAnchor == AnchorX.Left && xOffset < margin + snapRange) xOffset = margin;
        else if (xAnchor == AnchorX.Right && xOffset > -margin - width - snapRange) xOffset = -margin - width;

        double yCheck;
        switch (boxAnchorY) {
            default:     yCheck = yPos; break;
            case Center: yCheck = yPos + (height / 2.0); break;
            case Bottom: yCheck = yPos + height; break;
        }
        
        // Y
        switch (yAnchor) {
            case Top: {
                // Center
                if (yCheck > yThird) {
                    yAnchor = AnchorY.Center;
                    yOffset = yPos - (Utils.getWindowHeight() / 2.0);
                }

                break;
            }
            case Center: {
                // Top
                if (yCheck <= yThird) {
                    yAnchor = AnchorY.Top;
                    yOffset = yPos;
                }

                // Bottom
                else if (yCheck >= yThird * 2.0) {
                    yAnchor = AnchorY.Bottom;
                    yOffset = yPos - Utils.getWindowHeight();
                }

                break;
            }
            case Bottom: {
                // Center
                if (yPos < yThird * 2.0) {
                    yAnchor = AnchorY.Center;
                    yOffset = yPos - (Utils.getWindowHeight() / 2.0);
                }

                break;
            }
        }

        if (yAnchor == AnchorY.Top && yOffset < margin + snapRange) yOffset = margin;
        else if (yAnchor == AnchorY.Bottom && yOffset > -margin - height - snapRange) yOffset = -margin - height;
    }

    public void setSize(double width, double height) {
        if (this.width != width) {
            alignX(this.width, width);
            this.width = width;
        }
        if (this.height != height) {
            alignY(this.height, height);
            this.height = height;
        }

        setPos(getX(), getY());
    }

    private void alignX(double prevWidth, double newWidth) {
        switch (boxAnchorX) {
            case Center: setX(getX() + prevWidth / 2.0 - newWidth / 2.0); break;
            case Right:  setX(getX() + prevWidth - newWidth); break;
        }
    }

    private void alignY(double prevHeight, double newHeight) {
        switch (boxAnchorY) {
            case Center:    setY(getY() + prevHeight / 2.0 - newHeight / 2.0);
            case Bottom:    setY(getY() + prevHeight - newHeight);
        }
    }

    public boolean intersects(double x, double y) {
        double sx = getX();
        double sy = getY();

        return x >= sx && x <= sx + width && y >= sy && y <= sy + height;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("xAnchor", xAnchor.name());
        tag.putDouble("xOffset", xOffset);

        tag.putString("yAnchor", yAnchor.name());
        tag.putDouble("yOffset", yOffset);

        return tag;
    }

    @Override
    public BoundingBox fromTag(CompoundTag tag) {
        xAnchor = tag.contains("xAnchor") ? AnchorX.valueOf(tag.getString("xAnchor")) : AnchorX.Left;
        xOffset = tag.getDouble("xOffset");

        yAnchor = tag.contains("yAnchor") ? AnchorY.valueOf(tag.getString("yAnchor")) : AnchorY.Top;
        yOffset = tag.getDouble("yOffset");

        return this;
    }
}
