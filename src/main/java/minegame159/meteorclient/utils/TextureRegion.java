package minegame159.meteorclient.utils;

import java.util.Objects;

public class TextureRegion {
    public double x, y;
    public double width, height;

    private Color color, colorHovered, colorPressed;

    public TextureRegion(double textureWidth, double textuerHeight, int x, int y, int width, int height, Color color, Color colorHovered, Color colorPressed) {
        this.x = x / textureWidth;
        this.y = y / textuerHeight;
        this.width = width / textureWidth;
        this.height = height / textuerHeight;
        this.color = color;
        this.colorHovered = colorHovered;
        this.colorPressed = colorPressed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextureRegion that = (TextureRegion) o;
        return x == that.x &&
                y == that.y &&
                width == that.width &&
                height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }

    public Color getColor(boolean mouseOver, boolean pressed) {
        if (pressed) return colorPressed;
        if (mouseOver) return colorHovered;
        return color;
    }
}
