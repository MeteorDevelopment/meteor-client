/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.rendering.Matrices;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class VanillaTextRenderer implements TextRenderer {
    public static final TextRenderer INSTANCE = new VanillaTextRenderer();

    // Vanilla font is almost twice as small as our custom font (vanilla = 9, custom = 18)
    private double scale = 1.74;
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
    public double getWidth(String text, int length) {
        String string = text;
        if (length != string.length()) string = string.substring(0, length);
        return mc.textRenderer.getWidth(string) * scale;
    }

    @Override
    public double getHeight() {
        return mc.textRenderer.fontHeight * scale;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        // Vanilla renderer doesn't support batching

        // Vanilla font is twice as small as our custom font (vanilla = 9, custom = 18)
        this.scale = scale * 1.74;
        this.building = true;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        Matrices.push();
        Matrices.scale(scale, scale, 1);

        x += 0.5 * scale;
        y += 0.5 * scale;

        int preA = color.a;
        color.a = (int) ((color.a / 255 * alpha) * 255);

        RenderSystem.disableDepthTest();
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        double r = mc.textRenderer.draw(text, (float) (x / scale), (float) (y / scale), color.getPacked(), shadow, Matrices.getTop(), immediate, true, 0, 15728880);
        immediate.draw();
        RenderSystem.enableDepthTest();

        color.a = preA;

        Matrices.pop();
        return r * scale;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end(MatrixStack matrices) {
        // Vanilla renderer doesn't support batching

        // Vanilla font is twice as small as our custom font (vanilla = 9, custom = 18)
        this.scale = 1.74;
        this.building = false;
    }
}
