package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.utils.Color;

public class LineOperation extends Operation {
    private double x1, y1;
    private double x2, y2;
    private Color color;

    public LineOperation set(double x1, double y1, double x2, double y2, Color color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        ShapeBuilder.line(x1, y1, x2, y2, color);
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.lineOperationPool.free(this);
    }
}
