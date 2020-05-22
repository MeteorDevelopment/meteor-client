package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

public class TextOperation extends Operation {
    private String text;
    private double x, y;
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
        if (shadow) Utils.drawTextWithShadow(text, (float) x, (float) y, color.getPacked());
        else Utils.drawText(text, (float) x, (float) y, color.getPacked());
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.textOperationPool.free(this);
    }
}
