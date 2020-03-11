package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class WTextBox extends WWidget {
    public interface Filter {
        public boolean accept(WTextBox textBox, char c);
    }

    public String text;
    public Filter filter;
    public Consumer<WTextBox> action;

    private double maxCharCount;
    public boolean focused;
    private boolean cursorVisible;
    private int blinkTimer = 0;

    public WTextBox(String text, int maxCharCount) {
        boundingBox.setMargin(3);

        this.text = text != null ? text : "";
        this.maxCharCount = maxCharCount;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(maxCharCount * 6, Utils.getTextHeight());
    }

    @Override
    public boolean onMousePressed(int button) {
        focused = mouseOver;
        return false;
    }

    @Override
    public boolean onKeyPressed(int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && focused) {
            if (text.length() > 0) {
                text = text.substring(0, text.length() - 1);
                if (action != null) action.accept(this);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(char c) {
        if (focused) {
            if (text.length() < maxCharCount) {
                if (filter == null) {
                    text += c;
                    if (action != null) action.accept(this);
                } else if (filter.accept(this, c)) {
                    text += c;
                    if (action != null) action.accept(this);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public void onTick() {
        if (blinkTimer >= 10) {
            blinkTimer = 0;
            cursorVisible = !cursorVisible;
        }

        blinkTimer++;
    }

    @Override
    public void onRender(double delta) {
        renderBackground(GUI.backgroundTextBox, GUI.outline);

        if (focused && cursorVisible) {
            double textWidth = Utils.getTextWidth(text);
            RenderUtils.quad(boundingBox.getInnerX() + textWidth, boundingBox.getInnerY(), 1, Utils.getTextHeight(), GUI.text);
        }
    }

    @Override
    public void onRenderPost(double delta) {
        Utils.drawText(text, (float) boundingBox.getInnerX(), (float) boundingBox.getInnerY() + 1, GUI.textC);
    }
}
