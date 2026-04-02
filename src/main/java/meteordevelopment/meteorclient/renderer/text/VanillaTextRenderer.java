/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class VanillaTextRenderer implements TextRenderer {
    public static final VanillaTextRenderer INSTANCE = new VanillaTextRenderer();

    private final ByteBufferBuilder buffer = new ByteBufferBuilder(2048);
    private final MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);

    private final PoseStack matrices = new PoseStack();
    private final Matrix4f emptyMatrix = new Matrix4f();

    public double scale = 2;
    public boolean scaleIndividually;

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
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("VanillaTextRenderer.begin() called twice");

        this.scale = scale * 2;
        this.building = true;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        x += 0.5 * scale;
        y += 0.5 * scale;

        int preA = color.a;
        color.a = (int) (((double) color.a / 255 * alpha) * 255);

        Matrix4f matrix = emptyMatrix;
        if (scaleIndividually) {
            matrices.pushPose();
            matrices.scale((float) scale, (float) scale, 1);
            matrix = matrices.last().pose();
        }

        mc.font.drawInBatch(text, (float) (x / scale), (float) (y / scale), color.getPacked(), shadow, matrix, immediate, DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        double x2 = (x / scale) + mc.font.width(text);

        if (scaleIndividually) matrices.popPose();

        color.a = preA;

        if (!wasBuilding) end();
        return (x2 - 1) * scale;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("VanillaTextRenderer.end() called without calling begin()");

        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();

        matrixStack.pushMatrix();
        if (!scaleIndividually) matrixStack.scale((float) scale, (float) scale, 1);

        immediate.endBatch();

        matrixStack.popMatrix();

        this.scale = 2;
        this.building = false;
    }
}
