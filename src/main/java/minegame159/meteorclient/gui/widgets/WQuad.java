package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

public class WQuad extends WWidget {
    public Color color;

    public WQuad(Color color) {
        this.color = color;
    }

    @Override
    protected void onCalculateSize() {
        width = 4 + Utils.getTextHeight() + 4;
        height = 4 + Utils.getTextHeight() + 4;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderQuad(x, y, width, height, color);
    }
}
