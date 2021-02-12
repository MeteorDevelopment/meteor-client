/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.gui.screens.topbar.TopBarScreen;
import minegame159.meteorclient.gui.screens.topbar.TopBarType;
import minegame159.meteorclient.utils.render.color.Color;
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

    private static class WTopBarButton extends WPressable {
        private final TopBarType type;
        private final String name;

        private WTopBarButton(TopBarType type) {
            this.type = type;
            this.name = type.toString();
        }

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = 4 + renderer.textWidth(name) + 4;
            height = 4 + renderer.textHeight() + 4;
        }

        @Override
        protected void onAction(int button) {
            Screen screen = MinecraftClient.getInstance().currentScreen;

            if (!(screen instanceof TopBarScreen) || ((TopBarScreen) screen).type != type) {
                MinecraftClient mc = MinecraftClient.getInstance();

                double mouseX = mc.mouse.getX();
                double mouseY = mc.mouse.getY();

                if (screen != null) screen.onClose();
                mc.openScreen(type.createScreen());

                GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), mouseX, mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Color color = GuiConfig.get().background;
            if (pressed) color = GuiConfig.get().backgroundPressed;
            else if (mouseOver) color = GuiConfig.get().backgroundHovered;

            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof TopBarScreen && ((TopBarScreen) screen).type == type) color = GuiConfig.get().backgroundPressed;

            renderer.quad(Region.FULL, x, y, width, height, color);
            renderer.text(name, x + 4, y + 4, false, GuiConfig.get().text);
        }
    }
}
