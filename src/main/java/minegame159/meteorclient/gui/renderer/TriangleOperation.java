package minegame159.meteorclient.gui.renderer;

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
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));

        double oX = this.x + size / 2;
        double oY = this.y + size / 4;

        double x = ((this.x - oX) * cos) - ((this.y - oY) * sin) + oX;
        double y = ((this.y - oY) * cos) + ((this.x - oX) * sin) + oY;
        renderer.quadBuf.vertex(x, y, 0).texture(GuiRenderer.TEX_QUAD.x, GuiRenderer.TEX_QUAD.y).color(color.r, color.g, color.b, color.a).next();

        x = ((this.x + size - oX) * cos) - ((this.y - oY) * sin) + oX;
        y = ((this.y - oY) * cos) + ((this.x + size - oX) * sin) + oY;
        renderer.quadBuf.vertex(x, y, 0).texture(GuiRenderer.TEX_QUAD.x + GuiRenderer.TEX_QUAD.width, GuiRenderer.TEX_QUAD.y).color(color.r, color.g, color.b, color.a).next();

        x = ((this.x + size / 2 - oX) * cos) - ((this.y + size / 2 - oY) * sin) + oX;
        y = ((this.y + size / 2 - oY) * cos) + ((this.x + size / 2 - oX) * sin) + oY;
        renderer.quadBuf.vertex(x, y, 0).texture(GuiRenderer.TEX_QUAD.x + GuiRenderer.TEX_QUAD.width, GuiRenderer.TEX_QUAD.y + GuiRenderer.TEX_QUAD.height).color(color.r, color.g, color.b, color.a).next();
        renderer.quadBuf.vertex(x, y, 0).texture(GuiRenderer.TEX_QUAD.x, GuiRenderer.TEX_QUAD.y + GuiRenderer.TEX_QUAD.height).color(color.r, color.g, color.b, color.a).next();
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.triangleOperationPool.free(this);
    }
}
