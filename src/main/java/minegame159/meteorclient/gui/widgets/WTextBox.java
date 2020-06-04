package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiThings;
import minegame159.meteorclient.gui.listeners.TextBoxChangeListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class WTextBox extends WWidget {
    public interface Filter {
        public boolean accept(WTextBox textBox, char c);
    }

    public TextBoxChangeListener action;
    public Filter filter;

    public String text;

    private final double uWidth;
    private boolean focused;

    private int cursorTimer;
    private boolean cursorVisible;

    public WTextBox(String text, double uWidth) {
        this.text = text != null ? text : "";
        this.uWidth = uWidth;
    }

    @Override
    protected void onCalculateSize() {
        width = 3 + uWidth + 3;
        height = 3 + MeteorClient.FONT.getHeight() + 3;
    }

    @Override
    protected boolean onMouseClicked(int button) {
        if (!focused && mouseOver) GuiThings.setPostKeyEvents(true);
        else if (focused && !mouseOver) GuiThings.setPostKeyEvents(false);

        focused = mouseOver;

        if (focused && button == 1) {
            String preText = text;
            text = "";
            if (!preText.equals(text)) callAction();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onKeyPressed(int key, int mods) {
        if (focused && key == GLFW.GLFW_KEY_BACKSPACE && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
            callAction();
            return true;
        }

        if (key == GLFW.GLFW_KEY_V && mods == GLFW.GLFW_MOD_CONTROL && focused) {
            text += MinecraftClient.getInstance().keyboard.getClipboard();
            callAction();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onKeyRepeated(int key, int mods) {
        if (focused && key == GLFW.GLFW_KEY_BACKSPACE && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
            callAction();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onCharTyped(char c, int key) {
        if (focused) {
            if (filter == null) {
                text += c;
                callAction();
            } else if (filter.accept(this, c)) {
                text += c;
                callAction();
            }
            return true;
        }

        return false;
    }

    protected void callAction() {
        if (action != null) action.onTextBoxChange(this);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (!this.focused && focused) GuiThings.setPostKeyEvents(true);
        else if (this.focused && !focused) GuiThings.setPostKeyEvents(false);
        this.focused = focused;
    }

    @Override
    protected void onTick() {
        if (cursorTimer >= 10) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        } else {
            cursorTimer++;
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, false, false);

        double textWidth = MeteorClient.FONT.getStringWidth(text);

        double overflowWidth = textWidth - width + 3 + 3;
        if (overflowWidth < 0) overflowWidth = 0;

        if (text.length() > 0) {
            if (overflowWidth > 0) renderer.beginScissor(this, 0, 3, 0, 3, true);
            renderer.renderText(text, x + 3 - overflowWidth, y + 3, GuiConfig.INSTANCE.text, false);
            if (overflowWidth > 0) renderer.endScissor();
        }

        if (focused && cursorVisible) {
            renderer.renderQuad(x + 3 + textWidth - overflowWidth, y + 3, 1, MeteorClient.FONT.getHeight(), GuiConfig.INSTANCE.text);
        }
    }
}
