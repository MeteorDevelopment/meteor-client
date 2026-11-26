/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;
    private final String title;

    private double activeProgress;
    private double hoverProgress;

    public WMeteorModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        activeProgress = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(10);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double descriptionHeight = module.description.isBlank() ? 0 : theme.textHeight() + theme.scale(3);

        width = theme.scale(240);
        height = Math.max(theme.scale(54), pad + theme.textHeight() + descriptionHeight + pad);
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
        double border = theme.scale(1);
        double radius = theme.scale(theme.smallCornerRadius.get());

        activeProgress = approach(activeProgress, module.isActive() ? 1 : 0, delta, 4.0);
        hoverProgress = approach(hoverProgress, mouseOver ? 1 : 0, delta, 4.8);
        double activeEase = easeOutQuart(activeProgress);
        double hoverEase = easeOutQuart(hoverProgress);

        double glassOpacity = MeteorGuiTheme.GLASS_OPACITY;
        SettingColor outline = new SettingColor(theme.outlineColor.get(false, mouseOver));
        liftColor(outline, 0.08);
        outline.a = (int) (outline.a * (0.82 + 0.45 * hoverEase) * glassOpacity);

        SettingColor background = new SettingColor(
            module.isActive() ? theme.moduleBackground.get() : theme.backgroundColor.get(false, mouseOver)
        );
        liftColor(background, 0.12);
        double opacity = module.isActive()
            ? 0.72 + 0.10 * hoverEase
            : 0.54 + 0.12 * hoverEase;
        background.a = (int) (background.a * opacity * glassOpacity);

        drawRoundedRect(renderer, x, y, width, height, radius, outline);
        drawRoundedRect(renderer, x + border, y + border, width - border * 2, height - border * 2,
            Math.max(0, radius - border), background);

        if (activeEase > 0.01) {
            SettingColor activeColor = new SettingColor(theme.accentColor.get());
            activeColor.a = (int) (85 * activeEase * glassOpacity);
            renderer.quad(
                x + border,
                y + radius,
                theme.scale(2) * activeEase,
                height - radius * 2,
                activeColor
            );
        }

        double textX = x + pad + theme.scale(3);
        double titleY = y + pad;
        double maxTextWidth = Math.max(theme.scale(20), width - pad * 2 - theme.scale(6));

        renderer.text(fitText(title, maxTextWidth), textX, titleY,
            module.isActive() ? theme.accentColor.get() : theme.textColor.get(), false);

        if (!module.description.isBlank()) {
            double descriptionY = titleY + theme.textHeight() + theme.scale(3);
            renderer.text(fitText(module.description, maxTextWidth), textX, descriptionY,
                theme.textSecondaryColor.get(), false);
        }
    }

    private double approach(double value, double target, double delta, double speed) {
        return value + (target - value) * Math.min(1, Math.max(0, delta * speed));
    }

    private double easeOutQuart(double value) {
        double t = Math.max(0, Math.min(1, value));
        return 1 - Math.pow(1 - t, 4);
    }

    private void liftColor(SettingColor color, double amount) {
        color.r = (int) (color.r + (126 - color.r) * amount);
        color.g = (int) (color.g + (112 - color.g) * amount);
        color.b = (int) (color.b + (148 - color.b) * amount);
    }

    private String fitText(String value, double maxWidth) {
        if (theme.textWidth(value) <= maxWidth) return value;

        String ellipsis = "...";
        int length = value.length();
        while (length > 0 && theme.textWidth(value.substring(0, length) + ellipsis) > maxWidth) length--;
        return length == 0 ? "" : value.substring(0, length) + ellipsis;
    }

    private void drawRoundedRect(GuiRenderer renderer, double x, double y, double width, double height, double radius, meteordevelopment.meteorclient.utils.render.color.Color color) {
        double diameter = radius * 2;
        if (radius <= 0 || width <= diameter || height <= diameter) {
            renderer.quad(x, y, width, height, color);
            return;
        }

        renderer.quad(x + radius, y, width - diameter, height, color);
        renderer.quad(x, y + radius, width, height - diameter, color);
        renderer.quad(x, y, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x + width - diameter, y, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x, y + height - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x + width - diameter, y + height - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
    }
}
