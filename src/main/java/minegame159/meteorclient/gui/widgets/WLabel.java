/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class WLabel extends WWidget {
    public Color color;

    private String text;
    private boolean recalculate;
    private final List<String> lines;
    public boolean shadow;

    public double maxWidth;

    public WLabel(String text, boolean shadow) {
        this.lines = new ArrayList<>(1);
        this.shadow = shadow;

        this.color = GuiConfig.get().text;

        this.text = text;
        lines.add(text);
        recalculate = true;

        maxWidth = Math.max(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2, 512);
    }

    public WLabel(String text) {
        this(text, false);
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 0;
        height = renderer.textHeight() * lines.size();

        for (String line : lines) {
            width = Math.max(width, renderer.textWidth(line));
        }

        if (recalculate && width > maxWidth) {
            boolean split = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                double lastLineWidth = 0;
                for (int j = 0; j < line.length(); j++) {
                    double lineWidth = renderer.textWidth(line, j + 1);
                    if (lineWidth > maxWidth && lastLineWidth <= maxWidth) {
                        lines.add(i, line.substring(0, j));
                        lines.set(i + 1, line.substring(j));
                        split = true;
                    }
                    lastLineWidth = lineWidth;
                }
            }

            if (split) onCalculateSize(renderer);
        }
    }

    public void setText(String text) {
        this.text = text;
        lines.clear();
        lines.add(text);
        recalculate = true;
        invalidate();
    }

    public String getText() {
        return text;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double y = this.y;

        for (String line : lines) {
            renderer.text(line, x, y, shadow, color);
            y += renderer.textHeight();
        }
    }
}
