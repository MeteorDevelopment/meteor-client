/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class CustomTextRenderer implements TextRenderer {
    private static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final Mesh mesh = new ShaderMesh(Shaders.TEXT, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);

    private final Font[] fonts;
    private Font font;

    private boolean building;
    private boolean scaleOnly;
    private double scale = 1;

    public CustomTextRenderer(File file) {
        byte[] bytes = Utils.readBytes(file);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);

        fonts = new Font[5];
        for (int i = 0; i < fonts.length; i++) {
            ((Buffer) buffer).flip();
            fonts[i] = new Font(buffer, (int) Math.round(18 * ((i * 0.5) + 1)));
        }
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) mesh.begin();

        if (big) {
            this.font = fonts[fonts.length - 1];
        }
        else {
            double scaleA = Math.floor(scale * 10) / 10;

            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;

            font = fonts[scaleI - 1];
        }

        this.building = true;
        this.scaleOnly = scaleOnly;

        double fontScale = font.getHeight() / 18;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getWidth(text, length) + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public double getHeight(boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getHeight() + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font.render(mesh, text, x + 1, y + 1, SHADOW_COLOR, scale);
            font.render(mesh, text, x, y, color, scale);

            SHADOW_COLOR.a = preShadowA;
        }
        else {
            width = font.render(mesh, text, x, y, color, scale);
        }

        if (!wasBuilding) end();
        return width;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end(MatrixStack matrices) {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();

            GL.bindTexture(font.texture.getGlId());
            mesh.render(matrices);
        }

        building = false;
        scale = 1;
    }
}
