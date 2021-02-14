/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Utils;
import org.lwjgl.glfw.GLFW;

public class WModule extends WPressable {
    private final Module module;
    private double titleWidth;

    private double animationProgress1;
    private double animationMultiplier1;

    private double animationProgress2;
    private double animationMultiplier2;

    public WModule(Module module) {
        this.module = module;
        this.tooltip = module.description;

        if (module.isActive()) {
            animationProgress1 = 1;
            animationMultiplier1 = 1;

            animationProgress2 = 1;
            animationMultiplier2 = 1;
        } else {
            animationProgress1 = 0;
            animationMultiplier1 = -1;

            animationProgress2 = 0;
            animationMultiplier2 = -1;
        }
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        if (titleWidth == 0) titleWidth = renderer.textWidth(module.title);

        double s = GuiConfig.get().guiScale;
        width = 4 * s + titleWidth + 4 * s;
        height = 4 * s + renderer.textHeight() + 4 * s;
    }

    @Override
    protected void onAction(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) module.doAction(Utils.canUpdate());
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) module.openScreen();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (module.isActive()) {
            animationMultiplier1 = 1;
            animationMultiplier2 = 1;
        }
        else {
            animationMultiplier1 = -1;
            animationMultiplier2 = -1;
        }

        if (mouseOver) animationMultiplier1 = 1;
        else {
            if (!module.isActive()) animationMultiplier1 = -1;
        }

        animationProgress1 += delta / 10 * animationMultiplier1;
        animationProgress1 = Utils.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta / 10 * animationMultiplier2;
        animationProgress2 = Utils.clamp(animationProgress2, 0, 1);

        if (animationProgress1 > 0  || animationProgress2 > 0) {
            renderer.quad(Region.FULL, x, y, width * animationProgress1, height, GuiConfig.get().moduleBackground);
            renderer.quad(Region.FULL, x, y + height * (1 - animationProgress2), 2 * GuiConfig.get().guiScale, height * animationProgress2, GuiConfig.get().accent);
        }

        double nameX = x;

        switch (GuiConfig.get().moduleNameAlignment) {
            case Left:
                nameX += GuiConfig.get().moduleNameAlignmentPadding;
                break;
            case Center:
                nameX += + width / 2 - titleWidth / 2;
                break;
            case Right:
                nameX = (nameX + width - titleWidth) - GuiConfig.get().moduleNameAlignmentPadding;
                break;
        }

        renderer.text(module.title, nameX, y + 4, false, GuiConfig.get().text);
    }
}
