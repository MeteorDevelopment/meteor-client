package minegame159.meteorclient.clickgui.widgets;

import minegame159.meteorclient.clickgui.WidgetColors;
import minegame159.meteorclient.utils.Color;

public class Separator extends Widget {
    public Color color;
    public double shortening;

    private double fakeWidth;

    public Separator(double margin, Color color, double shortening) {
        super(0, 1, margin);
        this.color = color;
        this.shortening = shortening;
    }

    public Separator(double margin, Color color) {
        this(margin, color, 0);
    }

    public Separator(double margin) {
        this(margin, WidgetColors.separator);
    }

    public Separator() {
        this(0);
    }

    @Override
    public void layout() {
        super.layout();
        calculateSize();

        height = 1;
        if (parent != null) fakeWidth = parent.width;
    }

    @Override
    public void render(double mouseX, double mouseY) {
        quad(x + margin + shortening, y + margin, x + fakeWidth - shortening, y + height, color);

        super.render(mouseX, mouseY);
    }
}
