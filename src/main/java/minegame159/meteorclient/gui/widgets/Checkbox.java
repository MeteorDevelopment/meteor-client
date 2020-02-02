package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

import java.util.function.Consumer;

public class Checkbox extends Widget {
    public boolean checked;
    private Consumer<Checkbox> action;

    public Checkbox(double margin, boolean checked, Consumer<Checkbox> action) {
        super(Utils.getTextHeight() + 3 + 3, Utils.getTextHeight() + 3 + 3, margin);
        this.checked = checked;
        this.action = action;
    }

    public Checkbox(double margin, Consumer<Checkbox> action) {
        this(margin, false, action);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            checked = !checked;
            if (action != null) action.accept(this);
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(double mouseX, double mouseY) {
        Color backgroundColor = GUI.background;
        Color outlineColor = GUI.outline;
        if (isMouseOver(mouseX, mouseY)) {
            backgroundColor = GUI.backgroundHighlighted;
            outlineColor = GUI.outlineHighlighted;
        }

        renderBackgroundWithOutline(backgroundColor, outlineColor);
        if (checked) quad(x + margin + 4, y + margin + 4, x + width - 4, y + height - 4, GUI.checkbox);

        super.render(mouseX, mouseY);
    }
}
