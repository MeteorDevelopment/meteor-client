package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.listeners.ButtonClickListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.TextureRegion;
import minegame159.meteorclient.utils.Utils;

public class WButton extends WWidget {
    public ButtonClickListener action;

    private String text;
    private double textWidth;

    private TextureRegion tex;

    private boolean pressed;

    public WButton(String text, TextureRegion tex) {
        if (text != null) setText(text);
        else this.tex = tex;
    }

    public WButton(String text) {
        this(text, null);
    }

    public WButton(TextureRegion tex) {
        this(null, tex);
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.textWidth = Utils.getTextWidth(this.text);

        invalidate();
    }

    @Override
    protected void onCalculateSize() {
        width = 3 + (text != null ? Utils.getTextWidth(text) : Utils.getTextHeight()) + 3;
        height = 3 + Utils.getTextHeight() + 3;
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
            if (action != null) action.onButtonClick(this);
            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, mouseOver, pressed);

        if (text != null) {
            renderer.renderText(text, x + width / 2 - textWidth / 2, y + 3.5, GuiConfig.INSTANCE.text, false);
        } else {
            renderer.renderQuad(x + 3, y + 3, width - 6, height - 6, tex, tex.getColor(mouseOver, pressed));
        }
    }
}
