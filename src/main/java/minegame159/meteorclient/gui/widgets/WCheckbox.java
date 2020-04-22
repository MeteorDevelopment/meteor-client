package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.gui.listeners.CheckboxClickListener;

public class WCheckbox extends WWidget {
    public CheckboxClickListener action;

    public boolean checked;
    private boolean pressed;

    public WCheckbox(boolean checked) {
        this.checked = checked;
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
        if (mouseOver) {
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

        if (checked) {
            renderer.renderQuad(x + 3, y + 3, 8, 8, pressed ? GuiConfig.INSTANCE.checkboxPressed : GuiConfig.INSTANCE.checkbox);
        }
    }
}
