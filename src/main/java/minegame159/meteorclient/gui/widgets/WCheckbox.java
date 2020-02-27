package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Vector2;

import java.util.function.Consumer;

public class WCheckbox extends WWidget {
    public boolean checked;

    private Consumer<WCheckbox> action;

    public WCheckbox(boolean checked) {
        boundingBox.setMargin(3);

        this.checked = checked;
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            checked = !checked;
            if (action != null) action.accept(this);
            return true;
        }

        return false;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(6, 6);
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

        if (checked) RenderUtils.quad(boundingBox.getInnerX(), boundingBox.getInnerY(), boundingBox.innerWidth, boundingBox.innerHeight, GUI.checkbox);
    }

    public void setAction(Consumer<WCheckbox> action) {
        this.action = action;
    }
}
