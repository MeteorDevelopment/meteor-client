package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
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
        double s = GuiConfig.INSTANCE.guiScale;
        width = 6 * s + renderer.textHeight() + 6 * s;
        height = 6 * s + renderer.textHeight() + 6 * s;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(Region.FULL, x, y, width, height, color);
    }
}
