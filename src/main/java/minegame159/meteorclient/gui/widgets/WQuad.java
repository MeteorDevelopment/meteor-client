package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.Color;

public class WQuad extends WWidget {
    public Color color;

    public WQuad(Color color) {
        this.color = color;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 6 + renderer.textHeight() + 6;
        height = 6 + renderer.textHeight() + 6;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(Region.FULL, x, y, width, height, color);
    }
}
