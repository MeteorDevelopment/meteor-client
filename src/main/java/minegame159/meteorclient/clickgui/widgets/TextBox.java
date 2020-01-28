package minegame159.meteorclient.clickgui.widgets;

import minegame159.meteorclient.clickgui.WidgetColors;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.font.TextRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextBox extends Widget {
    public String text;
    private int maxCharCount;
    private TextBoxFilter filter;
    private Consumer<TextBox> action;

    private boolean focused;
    private double blinkTimer = 10;
    private boolean cursorVisible;

    public TextBox(double margin, String text, int maxCharCount, TextBoxFilter filter, Consumer<TextBox> action) {
        super(maxCharCount * 6, Utils.getTextHeight(), margin);
        this.text = text == null ? "" : text;
        this.maxCharCount = maxCharCount;
        this.filter = filter;
        this.action = action;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        focused = isMouseOver(mouseX, mouseY);

        return super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onKeyPressed(int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && focused && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
            if (action != null) action.accept(this);
            return true;
        }

        return super.onKeyPressed(key);
    }

    @Override
    public boolean onCharTyped(char c) {
        if (focused && text.length() + 1 <= maxCharCount) {
            if (filter != null && filter.accept(this, c)) {
                text += c;
                if (action != null) action.accept(this);
            } else if (filter == null) {
                text += c;
                if (action != null) action.accept(this);
            }

            return true;
        }

        return super.onCharTyped(c);
    }

    @Override
    public void tick() {
        if (blinkTimer <= 0) {
            blinkTimer = 10;
            cursorVisible = !cursorVisible;
        }

        blinkTimer -= 1;

        super.tick();
    }

    @Override
    public void render(double mouseX, double mouseY) {
        renderBackgroundWithOutline(WidgetColors.backgroundTextBox, WidgetColors.outline);

        // Cursor
        if (cursorVisible && focused) {
            double textWidth = Utils.getTextWidth(text);
            quad(x + margin + textWidth, y + margin, x + margin + textWidth + 1, y + margin + Utils.getTextHeight(), WidgetColors.text);
        }

        super.render(mouseX, mouseY);
    }

    @Override
    public void renderText(double mouseX, double mouseY, TextRenderer font) {
        font.draw(text, (float) (x + margin), (float) (y + margin + 1), WidgetColors.textC);

        super.renderText(mouseX, mouseY, font);
    }
}
