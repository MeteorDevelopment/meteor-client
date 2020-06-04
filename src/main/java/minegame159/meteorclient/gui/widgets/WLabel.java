package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.MeteorClient;
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
        width = MeteorClient.FONT.getStringWidth(text);
        height = MeteorClient.FONT.getHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderText(text, x, y, color, shadow);
    }
}
