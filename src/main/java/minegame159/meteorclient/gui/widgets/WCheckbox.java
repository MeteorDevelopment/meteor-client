package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;

import java.util.function.Consumer;

public class WCheckbox extends WWidget {
    public boolean checked;

    public double size = 6;
    private Consumer<WCheckbox> action;
    private double animationProgress;
    private double animationMultiplier;

    public WCheckbox(boolean checked) {
        boundingBox.setMargin(3);

        this.checked = checked;

        if (checked) {
            animationProgress = 1;
            animationMultiplier = 1;
        } else {
            animationProgress = 0;
            animationMultiplier = -1;
        }
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            checked = !checked;
            if (action != null) action.accept(this);
            animationMultiplier = checked ? 1 : -1;
            return true;
        }

        return false;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(size, size);
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

        animationProgress += delta * 0.6 * animationMultiplier;
        animationProgress = Utils.clamp(animationProgress, 0, 1);

        if (animationProgress > 0) {
            double w = boundingBox.innerWidth / 2;
            double h = boundingBox.innerHeight / 2;
            double cX = boundingBox.getInnerX() + w;
            double cY = boundingBox.getInnerY() + h;

            RenderUtils.quad(cX - w * animationProgress, cY - h * animationProgress, boundingBox.innerWidth * animationProgress, boundingBox.innerHeight * animationProgress, GUI.checkbox);
        }
    }

    public void setAction(Consumer<WCheckbox> action) {
        this.action = action;
    }
}
