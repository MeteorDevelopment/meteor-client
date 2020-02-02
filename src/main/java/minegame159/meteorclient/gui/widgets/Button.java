package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;

import java.util.function.Consumer;

public class Button extends Widget {
    private Label label;
    private Consumer<Button> action;

    public Button(double margin, String text, Consumer<Button> action) {
        super(0, 0, margin);
        this.action = action;

        label = new Label(0, text);
        addWidget(label);
    }

    public void setText(String text) {
        label.setText(text);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0 && action != null) {
            action.accept(this);
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void layout() {
        super.layout();
        calculateSize();
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

        super.render(mouseX, mouseY);
    }
}
