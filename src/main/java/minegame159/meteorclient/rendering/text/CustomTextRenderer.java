/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering.text;

import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class CustomTextRenderer implements TextRenderer {
    private static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final MeshBuilder mb = new MeshBuilder(16384);

    private final Font[] fonts;
    private Font font;

    private boolean building;
    private boolean scaleOnly;
    private double scale;

    public CustomTextRenderer(File file) {
        byte[] bytes = Utils.readBytes(file);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);

        fonts = new Font[5];
        for (int i = 0; i < fonts.length; i++) {
            ((Buffer) buffer).flip();
            fonts[i] = new Font(buffer, (int) Math.round(18 * ((i * 0.5) + 1)));
        }

        mb.texture = true;
    }

    @Override
    public void setAlpha(double a) {
        mb.alpha = a;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) mb.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR_TEXTURE);

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
    public double getWidth(String text, int length) {
        Font font = building ? this.font : fonts[0];
        return font.getWidth(text, length) * scale;
    }

    @Override
    public double getHeight() {
        Font font = building ? this.font : fonts[0];
        return font.getHeight() * scale;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double r;
        if (shadow) {
            r = font.render(mb, text, x + 1, y + 1, SHADOW_COLOR, scale);
            font.render(mb,text, x, y, color, scale);
        }
        else r = font.render(mb, text, x, y, color, scale);

        if (!wasBuilding) end();
        return r;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            font.texture.bindTexture();
            mb.end();
        }

        building = false;
        scale = 1;
    }
}
