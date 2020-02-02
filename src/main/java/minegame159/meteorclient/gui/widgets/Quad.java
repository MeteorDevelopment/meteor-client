package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Color;

public class Quad extends Widget {
    public Color color;

    public Quad(double width, double height, double margin, Color color) {
        super(width, height, margin);
        this.color = color;
    }

    @Override
    public void render(double mouseX, double mouseY) {
        quad(x, y, x + widthMargin(), y + heightMargin(), color);

        super.render(mouseX, mouseY);
    }
}
