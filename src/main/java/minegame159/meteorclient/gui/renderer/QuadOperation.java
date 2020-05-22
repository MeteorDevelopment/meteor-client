package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.TextureRegion;

public class QuadOperation extends Operation {
    private double x, y;
    private double width, height;
    private TextureRegion tex;
    private Color color1, color2, color3, color4;

    public QuadOperation set(double x, double y, double width, double height, TextureRegion tex, Color color1, Color color2, Color color3, Color color4) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tex = tex;
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.color4 = color4;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        TextureRegion tex = this.tex;
        if (tex == null) tex = GuiRenderer.TEX_QUAD;

        renderer.quadBuf.vertex(x, y, 0).texture((float) tex.x, (float) tex.y).color(color1.r, color1.g, color1.b, color1.a).next();
        renderer.quadBuf.vertex(x + width, y, 0).texture((float) (tex.x + tex.width), (float) tex.y).color(color2.r, color2.g, color2.b, color2.a).next();
        renderer.quadBuf.vertex(x + width, y + height, 0).texture((float) (tex.x + tex.width), (float) (tex.y + tex.height)).color(color3.r, color3.g, color3.b, color3.a).next();
        renderer.quadBuf.vertex(x, y + height, 0).texture((float) tex.x, (float) (tex.y + tex.height)).color(color4.r, color4.g, color4.b, color4.a).next();
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.quadOperationPool.free(this);
    }
}
