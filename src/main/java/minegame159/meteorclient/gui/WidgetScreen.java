package minegame159.meteorclient.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.renderer.GuiDebugRenderer;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WRoot;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public abstract class WidgetScreen extends Screen implements Listenable {
    private static final GuiRenderer GUI_RENDERER = new GuiRenderer();
    private static final GuiDebugRenderer GUI_DEBUG_RENDERER = new GuiDebugRenderer();

    public Screen parent;
    public final WWidget root;

    private final int prePostKeyEvents;
    private boolean firstInit = true;
    private boolean renderDebug = false;

    public boolean locked;

    public WidgetScreen(String title) {
        super(new LiteralText(title));

        this.parent = MinecraftClient.getInstance().currentScreen;
        this.root = new WFullScreenRoot();

        this.prePostKeyEvents = GuiKeyEvents.postKeyEvents;
    }

    public <T extends WWidget> Cell<T> add(T widget) {
        return root.add(widget);
    }

    public void clear() {
        root.clear();
    }

    @Override
    protected void init() {
        MeteorClient.EVENT_BUS.subscribe(this);

        if (firstInit) {
            firstInit = false;
            return;
        }

        loopWidget(root);
    }

    private void loopWidget(WWidget widget) {
        if (widget instanceof WTextBox && ((WTextBox) widget).isFocused()) GuiKeyEvents.setPostKeyEvents(true);

        for (Cell<?> cell : widget.getCells()) {
            loopWidget(cell.getWidget());
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (locked) return;

        double s = MinecraftClient.getInstance().getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        root.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (locked) return false;

        return root.mouseClicked(false, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (locked) return false;

        return root.mouseReleased(false, button);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double amount) {
        if (locked) return false;

        return root.mouseScrolled(amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (locked) return false;

        return super.keyPressed(keyCode, scanCode, modifiers) || root.keyPressed(keyCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (locked) return false;

        if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_9) {
            renderDebug = !renderDebug;
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public void keyRepeated(int key, int mods) {
        if (locked) return;

        root.keyRepeated(key, mods);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (locked) return false;

        return root.charTyped(chr, keyCode);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!Utils.canUpdate()) renderBackground(matrices);

        // Apply projection without scaling
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);

        Matrices.begin(new MatrixStack());

        // Render gui
        GUI_RENDERER.begin(true);
        root.render(GUI_RENDERER, mouseX, mouseY, delta);
        GUI_RENDERER.end(true);

        if (renderDebug) GUI_DEBUG_RENDERER.render(root);

        // Apply back original scaled projection
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / MinecraftClient.getInstance().getWindow().getScaleFactor(), MinecraftClient.getInstance().getWindow().getFramebufferHeight() / MinecraftClient.getInstance().getWindow().getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        root.invalidate();
    }

    @Override
    public void onClose() {
        if (locked) return;

        MeteorClient.EVENT_BUS.unsubscribe(this);
        GuiKeyEvents.postKeyEvents = prePostKeyEvents;
        MinecraftClient.getInstance().openScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !locked;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class WFullScreenRoot extends WWidget implements WRoot {
        private boolean valid = false;

        @Override
        public void invalidate() {
            valid = false;
        }

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
            height = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!valid) {
                calculateSize(renderer);
                calculateWidgetPositions();

                valid = true;
                mouseMoved(MinecraftClient.getInstance().mouse.getX(), MinecraftClient.getInstance().mouse.getY());
            }
        }
    }
}
