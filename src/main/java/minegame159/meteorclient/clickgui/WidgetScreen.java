package minegame159.meteorclient.clickgui;

import minegame159.meteorclient.clickgui.widgets.Widget;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;

public class WidgetScreen extends Screen {
    private static int bgColor = Color.fromRGBA(80, 80, 80, 80);

    private List<Widget> widgets = new ArrayList<>();

    public WidgetScreen(String title) {
        super(new LiteralText(title));
    }

    public void addWidget(Widget widget) {
        widgets.add(widget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Widget widget : widgets) {
            if (widget.onMouseClicked(mouseX, mouseY, button)) return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Widget widget : widgets) {
            if (widget.onMouseReleased(mouseX, mouseY, button)) return true;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (Widget widget : widgets) widget.onMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Widget widget : widgets) {
            if (widget.onKeyPressed(keyCode)) return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        for (Widget widget : widgets) {
            if (widget.onCharTyped(chr)) return true;
        }

        return false;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        for (Widget widget : widgets) widget.onWindowResized(width, height);

        super.resize(client, width, height);
    }

    @Override
    public void tick() {
        for (Widget widget : widgets) widget.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        fill(0, 0, width, height, bgColor);

        RenderUtils.beginQuads();
        for (Widget widget : widgets) widget.render(mouseX, mouseY);
        RenderUtils.endQuads();

        for (Widget widget : widgets) widget.renderText(mouseX, mouseY, font);
    }
}
