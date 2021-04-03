/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.themes.meteor.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import minegame159.meteorclient.gui.themes.meteor.MeteorWidget;
import minegame159.meteorclient.gui.utils.AlignmentX;
import minegame159.meteorclient.gui.widgets.pressable.WPressable;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;

import static minegame159.meteorclient.utils.Utils.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;

    private double titleWidth;

    private double animationProgress1;

    private double animationProgress2;

    public WMeteorModule(Module module) {
        this.module = module;

        if (module.isActive()) {
            animationProgress1 = 1;
            animationProgress2 = 1;
        } else {
            animationProgress1 = 0;
            animationProgress2 = 0;
        }
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (titleWidth == 0) titleWidth = theme.textWidth(module.title);

        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.doAction(Utils.canUpdate());
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.openScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();

        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = Utils.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = Utils.clamp(animationProgress2, 0, 1);

        if (animationProgress1 > 0) {
            renderer.quad(x, y, width * animationProgress1, height, theme.moduleBackground.get());
        }
        if (animationProgress2 > 0) {
            renderer.quad(x, y + height * (1 - animationProgress2), theme.scale(2), height * animationProgress2, theme.accentColor.get());
        }

        double x = this.x + pad;
        double w = width - pad * 2;

        if (theme.moduleAlignment.get() == AlignmentX.Center) {
            x += w / 2 - titleWidth / 2;
        }
        else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            x += w - titleWidth;
        }

        renderer.text(module.title, x, y + pad, theme.textColor.get(), false);
    }
}
