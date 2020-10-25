package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;

public class WVerticalSeparator extends WWidget {
    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 1;
        height = 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(Region.FULL, x, y, width, height, GuiConfig.INSTANCE.separator);
    }
}
