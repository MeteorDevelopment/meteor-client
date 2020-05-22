package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.listeners.CheckboxClickListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;

public class WCheckbox extends WWidget {
    public CheckboxClickListener action;

    public boolean checked;
    private boolean pressed;

    private double animationProgress;

    public WCheckbox(boolean checked) {
        this.checked = checked;
        this.animationProgress = checked ? 1 : 0;
    }

    @Override
    protected void onCalculateSize() {
        width = 3 + 8 + 3;
        height = 3 + 8 + 3;
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
            checked = !checked;
            if (action != null) action.onCheckboxClick(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, mouseOver, pressed);

        animationProgress += delta * (checked ? 1 : -1);
        animationProgress = Utils.clamp(animationProgress, 0, 1);

        if (animationProgress > 0) {
            renderer.renderQuad(x + 3 + 4 * (1 - animationProgress), y + 3 + 4 * (1 - animationProgress), 8 * animationProgress, 8 * animationProgress, pressed ? GuiConfig.INSTANCE.checkboxPressed : GuiConfig.INSTANCE.checkbox);
        }
    }
}
