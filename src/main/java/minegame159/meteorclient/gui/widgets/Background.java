package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;

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
        renderBackgroundWithOutline(GUI.background, GUI.outline);

        super.render(mouseX, mouseY);
    }
}
