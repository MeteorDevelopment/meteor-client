package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Vector2;

public class WMinus extends WWidget {
    public WButton.Action action;

    public WMinus() {
        boundingBox.setMargin(3);
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(6, 6);
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            if (action != null) action.clicked();
            return true;
        }

        return false;
    }

    @Override
    public void onRender(double delta) {
        Color background = GUI.background;
        Color outline = GUI.outline;
        if (mouseOver) {
            background = GUI.backgroundHighlighted;
            outline = GUI.outlineHighlighted;
        }
        renderBackground(background, outline);

        RenderUtils.quad(boundingBox.getInnerX(), boundingBox.getInnerY() + 2, 6, 2, GUI.minus);
    }
}
