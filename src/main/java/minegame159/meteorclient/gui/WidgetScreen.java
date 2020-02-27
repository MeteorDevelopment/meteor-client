package minegame159.meteorclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.gui.widgets.WDebugRenderer;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class WidgetScreen extends Screen {
    private WRoot root = new WRoot();
    private boolean renderDebug = false;

    public WidgetScreen(String title) {
        super(new LiteralText(title));

        WWidget trueRoot = new WWidget();
        trueRoot.layout = new TrueRootLayout();
        trueRoot.add(root);
    }

    public <T extends WWidget> T add(T widget) {
        return root.add(widget);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        root.mouseMove(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return root.mousePressed(button);
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
        return root.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        return root.charTyped(chr);
    }

    public void layout() {
        root.layout();
    }

    @Override
    public void tick() {
        root.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        RenderSystem.disableTexture();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableBlend();

        RenderUtils.beginQuads();
        root.render(delta);
        RenderUtils.endQuads();
        root.renderPost(delta, mouseX, mouseY);
        root.renderTooltip(mouseX, mouseY);

        if (renderDebug) WDebugRenderer.render(root, true);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        root.layout();
        root.mouseMove(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().getWindow().getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().getWindow().getScaleFactor());
        root.windowResized(width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class WRoot extends WWidget {
        @Override
        public Vector2 calculateCustomSize() {
            return new Vector2(MinecraftClient.getInstance().getWindow().getScaledWidth(), MinecraftClient.getInstance().getWindow().getScaledHeight());
        }
    }

    private static class TrueRootLayout extends WWidget.DefaultLayout {
        @Override
        public void reset(WWidget widget) {
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();
            box.width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            box.height = MinecraftClient.getInstance().getWindow().getScaledHeight();
        }
    }
}
