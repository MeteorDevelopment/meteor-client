package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.MixinValues;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

public class WTextBox extends WWidget {
    public interface Filter {
        public boolean accept(WTextBox textBox, char c);
    }

    public String text;
    public Filter filter;
    public Consumer<WTextBox> action;

    private double width;
    private boolean focused;
    private boolean cursorVisible;
    private int blinkTimer = 0;

    public WTextBox(String text, double width) {
        boundingBox.setMargin(3);

        this.text = text != null ? text : "";
        this.width = width;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(width, Utils.getTextHeight());
    }

    @Override
    public boolean onMousePressed(int button) {
        if (!focused && mouseOver) MixinValues.setPostKeyEvents(true);
        else if (focused && !mouseOver) MixinValues.setPostKeyEvents(false);

        focused = mouseOver;

        if (focused && button == 1) {
            text = "";
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(int key, int modifiers) {
        if (key == GLFW.GLFW_KEY_V && modifiers == GLFW.GLFW_MOD_CONTROL && focused) {
            text += MinecraftClient.getInstance().keyboard.getClipboard();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(char c, int key) {
        if (focused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && text.length() > 0) {
                text = text.substring(0, text.length() - 1);
                if (action != null) action.accept(this);
                return true;
            }

            if (filter == null) {
                text += c;
                if (action != null) action.accept(this);
            } else if (filter.accept(this, c)) {
                text += c;
                if (action != null) action.accept(this);
            }
            return true;
        }

        return false;
    }

    public void setFocused(boolean focused) {
        if (!this.focused && focused) MixinValues.setPostKeyEvents(true);
        else if (this.focused && !focused) MixinValues.setPostKeyEvents(false);
        this.focused = focused;
    }

    @Override
    public void onTick() {
        blinkTimer++;

        if (blinkTimer >= 10) {
            blinkTimer = 0;
            cursorVisible = !cursorVisible;
        }
    }

    @Override
    public void onRender(double delta) {
        renderBackground(GUI.backgroundTextBox, GUI.outline);

        if (focused && cursorVisible) {
            double textWidth = Utils.getTextWidth(text);

            double overflowWidth = textWidth - width;
            if (overflowWidth < 0) overflowWidth = 0;

            RenderUtils.quad(boundingBox.getInnerX() + textWidth - overflowWidth, boundingBox.getInnerY(), 1, Utils.getTextHeight(), GUI.text);
        }
    }

    @Override
    public void onRenderPost(double delta) {
        double overflowWidth = Utils.getTextWidth(text) - width;
        if (overflowWidth < 0) overflowWidth = 0;

        if (overflowWidth > 0) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            double scaleFactor = MinecraftClient.getInstance().window.getScaleFactor();
            GL11.glScissor((int) (boundingBox.getInnerX() * scaleFactor), (int) ((MinecraftClient.getInstance().window.getScaledHeight() - boundingBox.getInnerY() - boundingBox.innerHeight) * scaleFactor), (int) (boundingBox.innerWidth * scaleFactor), (int) (boundingBox.innerHeight * scaleFactor));
        }
        Utils.drawText(text, (float) (boundingBox.getInnerX() - overflowWidth), (float) boundingBox.getInnerY() + 1, GUI.textC);
        if (overflowWidth > 0) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
}
