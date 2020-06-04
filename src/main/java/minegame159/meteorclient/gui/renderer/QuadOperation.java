package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.rendering.ShapeBuilder;
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

        ShapeBuilder.texQuad(x, y, width, height, tex, color1, color2, color3, color4);
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.quadOperationPool.free(this);
    }
}
