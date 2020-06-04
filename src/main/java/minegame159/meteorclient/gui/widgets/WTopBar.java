package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.screens.topbar.TopBarScreen;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public class WTopBar extends WTable {
    public WTopBar() {
        defaultCell.space(0);

        for (TopBarType type : TopBarType.values()) {
            add(new WTopBarButton(type));
        }
    }

    private static class WTopBarButton extends WWidget {
        TopBarType type;

        boolean pressed;

        WTopBarButton(TopBarType type) {
            this.type = type;
        }

        @Override
        protected void onCalculateSize() {
            width = 2 + MeteorClient.FONT.getStringWidth(type.toString()) + 2;
            height = 2 + MeteorClient.FONT.getHeight() + 2;
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
                Screen screen = MinecraftClient.getInstance().currentScreen;
                if (!(screen instanceof TopBarScreen) || ((TopBarScreen) screen).type != type) {
                    MinecraftClient mc = MinecraftClient.getInstance();;

                    double mouseX = mc.mouse.getX();
                    double mouseY = mc.mouse.getY();

                    if (screen != null) screen.onClose();
                    mc.openScreen(type.createScreen());

                    GLFW.glfwSetCursorPos(mc.window.getHandle(), mouseX, mouseY);
                }
                return true;
            }

            return false;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Color color = GuiConfig.INSTANCE.background;
            if (pressed) color = GuiConfig.INSTANCE.backgroundPressed;
            else if (mouseOver) color = GuiConfig.INSTANCE.backgroundHovered;

            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof TopBarScreen && ((TopBarScreen) screen).type == type) color = GuiConfig.INSTANCE.backgroundPressed;

            renderer.renderQuad(x, y, width, height, color);
            renderer.renderText(type.toString(), x + 2, y + 2, GuiConfig.INSTANCE.text, false);
        }
    }
}
