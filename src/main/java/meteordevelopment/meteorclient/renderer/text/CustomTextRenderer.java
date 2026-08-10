/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class CustomTextRenderer implements TextRenderer {
    public static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
    private final Color shadowColor = new Color(SHADOW_COLOR);

    public final FontFace fontFace;

    private final List<ByteBuffer> fontBuffers;
    private final Font[] fonts;
    private Font font;

    private boolean building;
    private boolean scaleOnly;
    private boolean destroyed;
    private double fontScale = 1;
    private double scale = 1;

    public CustomTextRenderer(FontFace fontFace) throws IOException {
        this.fontFace = fontFace;
        this.fontBuffers = Fonts.readFontBuffers(fontFace);

        this.fonts = new Font[5];
        try {
            for (int i = 0; i < fonts.length; i++) {
                fonts[i] = createFont((int) Math.round(27 * ((i * 0.5) + 1)));
            }
        } catch (RuntimeException | Error e) {
            closeFonts();
            throw e;
        }
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (destroyed) throw new IllegalStateException("CustomTextRenderer has already been destroyed.");
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) mesh.begin();

        if (big) {
            this.font = fonts[fonts.length - 1];
        } else {
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

        this.fontScale = font.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        Font font = building ? this.font : fonts[0];
        return (font.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double getHeight(boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double width;
        if (shadow) {
            shadowColor.a = (int) (color.a / 255.0 * SHADOW_COLOR.a);

            width = font.render(mesh, text, x + fontScale * scale / 1.5, y + fontScale * scale / 1.5, shadowColor, scale / 1.5);
            font.render(mesh, text, x, y, color, scale / 1.5);
        } else {
            width = font.render(mesh, text, x, y, color, scale / 1.5);
        }

        if (!wasBuilding) end();
        return width;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        try {
            if (!scaleOnly) {
                mesh.end();
                font.uploadPendingGlyphs();

                MeshRenderer.begin()
                    .attachments(Minecraft.getInstance().getMainRenderTarget())
                    .pipeline(MeteorRenderPipelines.UI_TEXT)
                    .mesh(mesh)
                    .sampler("u_Texture", font.texture.getTextureView(), font.texture.getSampler())
                    .end();
            }
        } finally {
            building = false;
            scaleOnly = false;
            scale = 1;
        }
    }

    public Font createFont(int height) {
        if (destroyed) throw new IllegalStateException("CustomTextRenderer has already been destroyed.");
        return new Font(fontBuffers, height);
    }

    public void destroy() {
        if (destroyed) return;

        closeFonts();
        destroyed = true;
    }

    private void closeFonts() {
        for (Font font : fonts) {
            if (font != null) font.close();
        }
    }
}
