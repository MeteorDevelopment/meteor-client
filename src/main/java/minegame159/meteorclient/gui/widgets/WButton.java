package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;

public class WButton extends WWidget {
    public interface Action {
        public void clicked();
    }

    public Action action;

    protected WLabel label;

    public WButton(String text) {
        boundingBox.setMargin(3);
        boundingBox.autoSize = true;

        label = add(new WLabel(text));
        label.boundingBox.alignment.x = Alignment.X.Center;
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver && button == 0) {
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
    }
}
