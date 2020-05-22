package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

public class WLabel extends WWidget {
    public Color color;

    private String text;
    public boolean shadow;

    public WLabel(String text, boolean shadow) {
        this.text = text;
        this.shadow = shadow;

        color = GuiConfig.INSTANCE.text;
    }

    public WLabel(String text) {
        this(text, false);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    @Override
    protected void onCalculateSize() {
        width = Utils.getTextWidth(text);
        height = Utils.getTextHeight() + 2;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderText(text, x, y + 1, color, shadow);
    }
}
