package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.listeners.MinusClickListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Color;

public class WMinus extends WWidget {
    public MinusClickListener action;

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
        if (pressed) {
            pressed = false;
            if (action != null) action.onMinusClick(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, mouseOver, pressed);

        Color color = GuiConfig.INSTANCE.minus;
        if (pressed) color = GuiConfig.INSTANCE.minusPressed;
        else if (mouseOver) color = GuiConfig.INSTANCE.minusHovered;

        renderer.renderQuad(x + 4, y + 4 + 2, 6, 2, color);
    }
}
