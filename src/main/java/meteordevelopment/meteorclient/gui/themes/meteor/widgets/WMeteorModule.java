/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;
    private final String title;

    private double titleWidth;

    private double animationProgress1;

    private double animationProgress2;

    public WMeteorModule(Module module, String title) {
        this.module = module;
        this.title = title;
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

        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();

        // Smooth animations
        animationProgress1 += delta * 6 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = Mth.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 10 * (module.isActive() ? 1 : -1);
        animationProgress2 = Mth.clamp(animationProgress2, 0, 1);

        // Eased animation
        double eased1 = animationProgress1 * animationProgress1 * (3 - 2 * animationProgress1);
        double eased2 = animationProgress2 * animationProgress2 * (3 - 2 * animationProgress2);

        Color accent = theme.accentColor.get();

        // Hover effect - full width glow
        if (eased1 > 0) {
            Color hoverBg = new Color(accent.r, accent.g, accent.b, (int)(30 * eased1));
            renderer.quad(x, y, width, height, hoverBg);
        }

        // Active state - vibrant left bar + glow
        if (eased2 > 0) {
            // Left accent bar
            renderer.quad(x, y, 3, height, new Color(accent.r, accent.g, accent.b, (int)(255 * eased2)));

            // Horizontal glow from left bar
            Color glowStart = new Color(accent.r, accent.g, accent.b, (int)(60 * eased2));
            Color glowEnd = new Color(accent.r, accent.g, accent.b, 0);
            renderer.quad(x + 3, y, width * 0.4, height, glowStart, glowEnd, glowEnd, glowStart);

            // Bottom line
            Color bottomLine = new Color(accent.r, accent.g, accent.b, (int)(80 * eased2));
            renderer.quad(x, y + height - 1, width, 1, bottomLine);
        }

        double x = this.x + pad + (eased2 > 0 ? 4 : 0); // Indent when active
        double w = width - pad * 2;

        if (theme.moduleAlignment.get() == AlignmentX.Center) {
            x += w / 2 - titleWidth / 2;
        } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            x += w - titleWidth;
        }

        // Text color - accent tint when active
        Color textColor = theme.textColor.get();
        if (eased2 > 0) {
            textColor = new Color(
                (int)(textColor.r + (accent.r - textColor.r) * 0.5 * eased2),
                (int)(textColor.g + (accent.g - textColor.g) * 0.4 * eased2),
                (int)(textColor.b + (accent.b - textColor.b) * 0.3 * eased2),
                textColor.a
            );
        }
        renderer.text(title, x, y + pad, textColor, false);
    }
}
