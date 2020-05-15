package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.listeners.PlusClickListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Color;

public class WPlus extends WWidget {
    public PlusClickListener action;

    private boolean pressed;

    @Override
    protected void onCalculateSize() {
        width = 4 + 6 + 4;
        height = 4 + 6 + 4;
    }

    @Override
    protected boolean onMouseClicked(int button) {
        if (mouseOver) {
            pressed = true;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseReleased(int button) {
        if (mouseOver) {
            pressed = false;
            if (action != null) action.onPlusClick(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, mouseOver, pressed);

        Color color = GuiConfig.INSTANCE.plus;
        if (pressed) color = GuiConfig.INSTANCE.plusPressed;
        else if (mouseOver) color = GuiConfig.INSTANCE.plusHovered;

        renderer.renderQuad(x + 4, y + 4 + 2, 6, 2, color);
        renderer.renderQuad(x + 4 + 2, y + 4, 2, 6, color);
    }
}
