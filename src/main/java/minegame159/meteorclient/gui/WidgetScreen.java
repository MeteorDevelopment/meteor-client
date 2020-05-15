package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class WidgetScreen extends Screen {
    public final String title;
    protected final MinecraftClient mc;

    public Screen parent;
    public final WWidget root;
    private int prePostKeyEvents;
    private boolean renderDebug = false;

    public WidgetScreen(String title) {
        super(new LiteralText(title));

        this.title = title;
        this.mc = MinecraftClient.getInstance();
        this.parent = mc.currentScreen;
        this.root = new WRoot();
        this.prePostKeyEvents = GuiThings.postKeyEvents;
    }

    public <T extends WWidget> Cell<T> add(T widget) {
        return root.add(widget);
    }

    public void clear() {
        root.clear();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        root.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return root.mouseClicked(button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return root.mouseReleased(button);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double amount) {
        return root.mouseScrolled(amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_9) {
            renderDebug = !renderDebug;
            return true;
        }

        return root.keyPressed(keyCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void keyRepeated(int key, int mods) {
        root.keyRepeated(key, mods);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        return root.charTyped(chr, keyCode);
    }

    @Override
    public void tick() {
        root.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        if (!Utils.canUpdate()) renderBackground();

        GuiRenderer.INSTANCE.begin();
        root.render(GuiRenderer.INSTANCE, mouseX, mouseY, delta);
        GuiRenderer.INSTANCE.end();

        if (renderDebug) GuiRenderer.INSTANCE.renderDebug(root);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        root.invalidate();
    }

    @Override
    public void onClose() {
        GuiThings.postKeyEvents = prePostKeyEvents;
        mc.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class WRoot extends WWidget {
        @Override
        protected void onCalculateSize() {
            width = mc.window.getScaledWidth();
            height = mc.window.getScaledHeight();
        }
    }
}
