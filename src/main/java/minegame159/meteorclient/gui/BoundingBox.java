package minegame159.meteorclient.gui;

import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

public class BoundingBox {
    public static interface CalculateCustomSize {
        public Vector2 calculate();
    }

    public Alignment alignment = new Alignment();
    public boolean autoSize, calculateAutoSizePost;
    public boolean fullWidth;

    public double x, y;
    public double innerWidth, innerHeight;

    public double marginLeft, marginRight;
    public double marginTop, marginBottom;

    private CalculateCustomSize calculateCustomSize;

    public BoundingBox(CalculateCustomSize calculateCustomSize) {
        this.calculateCustomSize = calculateCustomSize;
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + getWidth() && y >= this.y && y <= this.y + getHeight();
    }

    public void calculateCustomSize() {
        Vector2 customSize = calculateCustomSize.calculate();
        innerWidth = customSize.x;
        innerHeight = customSize.y;
    }

    public void calculatePos(Box box) {
        if (fullWidth) innerWidth = box.width - marginLeft - marginRight;

        switch (alignment.x) {
            case Left:   x = box.x; break;
            case Center: x = box.x + box.width / 2.0 - getWidth() / 2.0; break;
            case Right:  x = box.x + box.width - getWidth(); break;
        }

        switch (alignment.y) {
            case Top:    y = box.y; break;
            case Center: y = box.y + box.height / 2.0 - getHeight() / 2.0; break;
            case Bottom: y = box.y + box.height - getHeight(); break;
        }
    }

    public void setMargin(double margin) {
        marginLeft = margin;
        marginRight = margin;
        marginTop = margin;
        marginBottom = margin;
    }
    public void setMargin(double horizontalMargin, double verticalMargin) {
        marginLeft = horizontalMargin;
        marginRight = horizontalMargin;
        marginTop = verticalMargin;
        marginBottom = verticalMargin;
    }

    public double getInnerX() {
        return x + marginLeft;
    }
    public double getInnerY() {
        return y + marginTop;
    }

    public double getWidth() {
        return marginLeft + innerWidth + marginRight;
    }
    public double getHeight() {
        return marginTop + innerHeight + marginBottom;
    }
}
