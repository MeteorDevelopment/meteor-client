/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.Tabs;
import minegame159.meteorclient.gui.themes.meteor.MeteorWidget;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.pressable.WPressable;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;

import static minegame159.meteorclient.utils.Utils.mc;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

public abstract class WTopBar extends WHorizontalList {
    protected abstract Color getButtonColor(boolean pressed, boolean hovered);

    protected abstract Color getNameColor();
    protected abstract int getRoundingFactor();

    public WTopBar() {
        spacing = 0;
    }

    @Override
    public void init() {
        for (Tab tab : Tabs.get()) {
            add(new WTopBarButton(tab));
        }
    }

    protected int getState(WTopBarButton btn) {
        int a = 0;
        if (btn.equals(cells.get(0).widget()))
            a |= 1;
        if (btn.equals(cells.get(cells.size() - 1).widget()))
            a |= 2;
        return a;
    }

    protected class WTopBarButton extends WPressable {
        private final Tab tab;

        public WTopBarButton(Tab tab) {
            this.tab = tab;
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(tab.name) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.currentScreen;

            if (!(screen instanceof TabScreen) || ((TabScreen) screen).tab != tab) {
                double mouseX = mc.mouse.getX();
                double mouseY = mc.mouse.getY();

                tab.openScreen(theme);
                glfwSetCursorPos(mc.getWindow().getHandle(), mouseX, mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double pad = pad();
            Color color = getButtonColor(pressed || (mc.currentScreen instanceof TabScreen && ((TabScreen) mc.currentScreen).tab == tab), mouseOver);

            //renderer.quad(x, y, width, height, color);
            switch (getState(this)) {
                case 0:
                    renderer.quad(this, color);
                    break;
                case 1:
                    renderer.quadRoundedSide(this, color, getRoundingFactor(), false);
                    break;
                case 2:
                    renderer.quadRoundedSide(this, color, getRoundingFactor(), true);
                    break;
                case 3:
                    renderer.quadRounded(this, color, getRoundingFactor());
                    break;
            }
            renderer.text(tab.name, x + pad, y + pad, getNameColor(), false);
        }
    }
}
