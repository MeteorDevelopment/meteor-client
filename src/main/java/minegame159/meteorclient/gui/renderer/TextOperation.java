package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.utils.Color;

public class TextOperation extends Operation {
    protected String text;
    protected double x, y;
    private Color color;
    private boolean shadow;

    public TextOperation set(String text, double x, double y, Color color, boolean shadow) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shadow = shadow;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        if (shadow) MeteorClient.FONT.renderStringWithShadow(text, (float) x, (float) y, color);
        else MeteorClient.FONT.renderString(text, (float) x, (float) y, color);
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.textOperationPool.free(this);
    }
}
