package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.gui.listeners.TriangleClickListener;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

public class WTriangle extends WWidget {
    public TriangleClickListener action;

    public boolean checked;

    private boolean pressed;
    private double angle;

    @Override
    protected void onCalculateSize() {
        width = 10;
        height = 10;
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
            checked = !checked;
            if (action != null) action.onTriangleClick(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Color color = GuiConfig.INSTANCE.background;
        if (pressed) color = GuiConfig.INSTANCE.backgroundPressed;
        else if (mouseOver) color = GuiConfig.INSTANCE.backgroundHovered;

        angle += delta * 40 * (checked ? -1 : 1);
        angle = Utils.clamp(angle, -90, 0);

        renderer.renderTriangle(x, y + height / 4, width, angle, color);
    }
}
