package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;

public class WVerticalSeparator extends WWidget {
    @Override
    protected void onCalculateSize() {
        width = 1;
        height = 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderQuad(x, y, width, height, GuiConfig.INSTANCE.separator);
    }
}
