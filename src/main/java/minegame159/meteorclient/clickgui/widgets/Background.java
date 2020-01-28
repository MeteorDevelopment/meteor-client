package minegame159.meteorclient.clickgui.widgets;

import minegame159.meteorclient.clickgui.WidgetColors;

public class Background extends Widget {
    public Background(double margin) {
        super(0, 0, margin);
    }

    @Override
    public void layout() {
        super.layout();
        calculateSize();
    }

    @Override
    public void render(double mouseX, double mouseY) {
        renderBackgroundWithOutline(WidgetColors.background, WidgetColors.outline);

        super.render(mouseX, mouseY);
    }
}
