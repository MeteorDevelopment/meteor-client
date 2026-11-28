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
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;
    private final String title;

    private double titleWidth;
    private final double maxTitleWidth;
    private String displayTitle;

    private double animationProgress1;

    private double animationProgress2;

    public WMeteorModule(Module module, String title, double maxTitleWidth) {
        this.module = module;
        this.title = title;
        this.maxTitleWidth = maxTitleWidth;
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

        if (titleWidth == 0) {
            titleWidth = theme.textWidth(title);

            // Fenster sollen auf jeden Bildschirm passen, lieber ein gekürzter Titel als ein abgeschnittener
            if (maxTitleWidth > 0 && titleWidth > maxTitleWidth) {
                displayTitle = truncate(title, maxTitleWidth);
                titleWidth = theme.textWidth(displayTitle);
            }
        }

        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    private String truncate(String text, double maxWidth) {
        for (int length = text.length() - 1; length > 0; length--) {
            if (theme.textWidth(text, length, false) + theme.textWidth("…") <= maxWidth) {
                return text.substring(0, length).stripTrailing() + "…";
            }
        }

        return "…";
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

        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = Mth.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = Mth.clamp(animationProgress2, 0, 1);

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
        } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            x += w - titleWidth;
        }

        renderer.text(displayTitle == null ? title : displayTitle, x, y + pad, theme.textColor.get(), false);
    }
}
