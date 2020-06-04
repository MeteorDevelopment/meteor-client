package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.utils.Color;

public class TriangleOperation extends Operation {
    private double x, y;
    private double size;
    private double angle;
    private Color color;

    public TriangleOperation set(double x, double y, double size, double angle, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.angle = angle;
        this.color = color;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        ShapeBuilder.triangle(x, y, size, angle, color);
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.triangleOperationPool.free(this);
    }
}
