/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class VanillaTextRenderer implements TextRenderer {
    public static final VanillaTextRenderer INSTANCE = new VanillaTextRenderer();

    public double scale = 2;
    public boolean scaleIndividually;

    private GuiGraphicsExtractor graphics;
    private boolean building;
    private double alpha = 1;

    private VanillaTextRenderer() {
        // Use INSTANCE
    }

    @Override
    public void setAlpha(double a) {
        alpha = a;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        if (length != text.length()) text = text.substring(0, length);
        return (mc.font.width(text) + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public double getHeight(boolean shadow) {
        return (mc.font.lineHeight + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public void begin(GuiGraphicsExtractor graphics, double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("VanillaTextRenderer.begin() called twice");

        this.graphics = graphics;
        this.scale = scale * 2;
        this.building = true;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        if (!building) throw new RuntimeException("VanillaTextRenderer.render() called without calling begin()");

        x += 0.5 * scale;
        y += 0.5 * scale;

        int preA = color.a;
        color.a = (int) (((double) color.a / 255 * alpha) * 255);

        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();

        matrices.scale((float) scale, (float) scale);

        graphics.text(mc.font, text, (int) (x / scale), (int) (y / scale), color.getPacked());
        double x2 = (x / scale) + mc.font.width(text);

        matrices.popMatrix();

        color.a = preA;

        return (x2 - 1) * scale;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("VanillaTextRenderer.end() called without calling begin()");

        this.graphics = null;
        this.scale = 2;
        this.building = false;
    }
}
