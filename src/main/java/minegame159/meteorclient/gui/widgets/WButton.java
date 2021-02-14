/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.render.color.Color;

public class WButton extends WPressable {
    public enum ButtonRegion {
        Edit(Region.EDIT, GuiConfig.get().edit, GuiConfig.get().editHovered, GuiConfig.get().editPressed),
        Reset(Region.RESET, GuiConfig.get().reset, GuiConfig.get().resetHovered, GuiConfig.get().resetPressed);

        public final Region region;
        public final Color color, hovered, pressed;

        ButtonRegion(Region region, Color color, Color hovered, Color pressed) {
            this.region = region;
            this.color = color;
            this.hovered = hovered;
            this.pressed = pressed;
        }
    }

    private String text;

    private final ButtonRegion region;

    public WButton(String text, ButtonRegion region) {
        this.text = text;

        this.region = region;
    }

    public WButton(String text) {
        this(text, null);
    }

    public WButton(ButtonRegion region) {
        this(null, region);
    }

    public void setText(String text) {
        this.text = text;

        invalidate();
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        double s = GuiConfig.get().guiScale;
        width = 6 * s + (text == null ? renderer.textHeight() : renderer.textWidth(text)) + 6 * s;
        height = 6 * s + renderer.textHeight() + 6 * s;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.background(this, super.pressed);

        double s = GuiConfig.get().guiScale;

        if (text != null) {
            renderer.text(text, x + width / 2 - renderer.textWidth(text) / 2, y + 6 * s, false, GuiConfig.get().text);
        } else {
            Color color;
            if (pressed) color = region.pressed;
            else if (mouseOver) color = region.hovered;
            else color = region.color;

            renderer.quad(region.region, x + 6 * s, y + 6 * s, renderer.textHeight(), renderer.textHeight(), color);
        }
    }
}
