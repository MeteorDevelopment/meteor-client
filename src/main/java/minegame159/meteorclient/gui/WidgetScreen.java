package minegame159.meteorclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.widgets.WDebugRenderer;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class WidgetScreen extends Screen {
    public Screen parent;
    private WRoot root = new WRoot();
    private boolean renderDebug = false;

    protected MinecraftClient mc;

    public WidgetScreen(String title) {
        super(new LiteralText(title));
        mc = MinecraftClient.getInstance();

        parent = mc.currentScreen instanceof WidgetScreen ? mc.currentScreen : null;

        WWidget trueRoot = new WWidget();
        trueRoot.layout = new TrueRootLayout();
        trueRoot.add(root);
    }

    public <T extends WWidget> T add(T widget) {
        return root.add(widget);
    }

    public void clear() {
        root.widgets.clear();
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
        root.mouseMove(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().window.getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().window.getScaleFactor());
    }

    @Override
    public void tick() {
        root.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        if (!Utils.canUpdate()) renderBackground();

        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        GlStateManager.pushMatrix();
        GL11.glLineWidth(1);
        RenderUtils.beginLines();
        RenderUtils.beginQuads();
        root.render(delta);
        RenderUtils.endQuads();
        RenderUtils.endLines();
        GlStateManager.enableTexture();
        root.renderPost(delta, mouseX, mouseY);
        root.renderTooltip(mouseX, mouseY);
        GlStateManager.popMatrix();

        if (renderDebug) WDebugRenderer.render(root, true);

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        layout();
        root.windowResized(width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.openScreen(parent);
    }

    private static class WRoot extends WWidget {
        @Override
        public Vector2 calculateCustomSize() {
            return new Vector2(MinecraftClient.getInstance().window.getScaledWidth(), MinecraftClient.getInstance().window.getScaledHeight());
        }
    }

    private static class TrueRootLayout extends WWidget.DefaultLayout {
        @Override
        public void reset(WWidget widget) {
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();
            box.width = MinecraftClient.getInstance().window.getScaledWidth();
            box.height = MinecraftClient.getInstance().window.getScaledHeight();
        }
    }
}
