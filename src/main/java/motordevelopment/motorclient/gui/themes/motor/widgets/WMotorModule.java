/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor.widgets;

import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.themes.motor.MotorGuiTheme;
import motordevelopment.motorclient.gui.themes.motor.MotorWidget;
import motordevelopment.motorclient.gui.utils.AlignmentX;
import motordevelopment.motorclient.gui.widgets.pressable.WPressable;
import motordevelopment.motorclient.systems.modules.Module;
import net.minecraft.util.math.MathHelper;

import static motordevelopment.motorclient.MotorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WMotorModule extends WPressable implements MotorWidget {
    private final Module module;

    private double titleWidth;

    private double animationProgress1;

    private double animationProgress2;

    public WMotorModule(Module module) {
        this.module = module;
        this.tooltip = module.description;

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
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MotorGuiTheme theme = theme();
        double pad = pad();

        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = MathHelper.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = MathHelper.clamp(animationProgress2, 0, 1);

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
