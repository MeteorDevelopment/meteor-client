/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.gui.renderer.packer.TextureRegion;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.world.Dir;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;

import static org.lwjgl.opengl.GL11.*;

public class MeshBuilder {
    private final BufferBuilder buffer;
    private double offsetX, offsetY, offsetZ;

    public double alpha = 1;

    public boolean depthTest = false;
    public boolean texture = false;

    private int count;

    public MeshBuilder(int initialCapacity) {
        buffer = new BufferBuilder(initialCapacity);
    }

    public MeshBuilder() {
        buffer = new BufferBuilder(2097152);
    }

    public void begin(RenderEvent event, DrawMode drawMode, VertexFormat format) {
        if (event != null) {
            offsetX = -event.offsetX;
            offsetY = -event.offsetY;
            offsetZ = -event.offsetZ;
        } else {
            offsetX = 0;
            offsetY = 0;
            offsetZ = 0;
        }

        buffer.begin(drawMode.toOpenGl(), format);
        count = 0;
    }

    public void end() {
        buffer.end();

        //if (count > 0) {
            glPushMatrix();
            RenderSystem.multMatrix(Matrices.getTop());

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            RenderSystem.disableAlphaTest();
            if (texture) RenderSystem.enableTexture();
            else RenderSystem.disableTexture();
            RenderSystem.disableLighting();
            RenderSystem.disableCull();
            glEnable(GL_LINE_SMOOTH);
            RenderSystem.lineWidth(1);
            RenderSystem.color4f(1, 1, 1, 1);
            GlStateManager.shadeModel(GL_SMOOTH);

            BufferRenderer.draw(buffer);

            RenderSystem.enableAlphaTest();
            RenderSystem.enableDepthTest();
            RenderSystem.enableTexture();
            glDisable(GL_LINE_SMOOTH);

            glPopMatrix();
        //}
    }

    public boolean isBuilding() {
        return buffer.isBuilding();
    }

    public MeshBuilder pos(double x, double y, double z) {
        buffer.vertex(x + offsetX, y + offsetY, z + offsetZ);
        return this;
    }

    public MeshBuilder texture(double x, double y) {
        buffer.texture((float) x, (float) y);
        return this;
    }

    public MeshBuilder color(Color color) {
        buffer.color(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f * (float) alpha);
        return this;
    }

    public MeshBuilder color(int color) {
        buffer.color(Color.toRGBAR(color) / 255f, Color.toRGBAG(color) / 255f, Color.toRGBAB(color) / 255f, Color.toRGBAA(color) / 255f * (float) alpha);
        return this;
    }

    public void endVertex() {
        buffer.next();
    }

    // Quads, 2 dimensional, top left to bottom right

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        pos(x1, y1, z1).color(topLeft).endVertex();
        pos(x2, y2, z2).color(topRight).endVertex();
        pos(x3, y3, z3).color(bottomRight).endVertex();
        pos(x1, y1, z1).color(topLeft).endVertex();
        pos(x3, y3, z3).color(bottomRight).endVertex();
        pos(x4, y4, z4).color(bottomLeft).endVertex();
    }

    public void quad(double x, double y, double width, double height, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        quad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, topLeft, topRight, bottomRight, bottomLeft);
    }

    public void verticalGradientQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color top, Color bottom) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, top, top, bottom, bottom);
    }

    public void verticalGradientQuad(double x, double y, double width, double height, Color top, Color bottom) {
        verticalGradientQuad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, top, bottom);
    }

    public void horizontalGradientQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color left, Color right) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, left, right, right, left);
    }

    public void horizontalGradientQuad(double x, double y, double width, double height, Color left, Color right) {
        horizontalGradientQuad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, left, right);
    }

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, color, color, color);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, color);
    }

    public void horizontalQuad(double x1, double z1, double x2, double z2, double y, Color color) {
        quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color);
    }

    public void verticalQuad(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    public void texQuad(double x, double y, double width, double height, TextureRegion tex, Color color) {
        pos(x, y, 0).color(color).texture(tex.x1, tex.y1).endVertex();
        pos(x + width, y, 0).color(color).texture(tex.x2, tex.y1).endVertex();
        pos(x + width, y + height, 0).color(color).texture(tex.x2, tex.y2).endVertex();

        pos(x, y, 0).color(color).texture(tex.x1, tex.y1).endVertex();
        pos(x + width, y + height, 0).color(color).texture(tex.x2, tex.y2).endVertex();
        pos(x, y + height, 0).color(color).texture(tex.x1, tex.y2).endVertex();
    }

    public void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        if (Dir.is(excludeDir, Dir.DOWN)) quad(x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, color); // Bottom
        if (Dir.is(excludeDir, Dir.UP)) quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color); // Top

        if (Dir.is(excludeDir, Dir.NORTH)) quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color); // Front
        if (Dir.is(excludeDir, Dir.SOUTH)) quad(x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, color); // Back

        if (Dir.is(excludeDir, Dir.WEST)) quad(x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, color); // Left
        if (Dir.is(excludeDir, Dir.EAST)) quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color); // Right
    }

    // Rounded quad

    public void quadRoundedOutline(double x, double y, double width, double height, Color color, int r, double s) {
        r = getR(r, width, height);
        if (r == 0) {
            quad(x, y, width, s, color);
            quad(x, y + height - s, width, s, color);
            quad(x, y + s, s, height - s * 2, color);
            quad(x + width - s, y + s, s, height - s * 2, color);
        }
        else {
            int cirDepth = getCir(r);
            //top
            quarterCircleOutline(x + r, y + r, r, 3, cirDepth, color, s);
            quad(x + r, y, width - r * 2, s, color);
            quarterCircleOutline(x + width - r, y + r, r, 0, cirDepth, color, s);
            //middle
            quad(x, y + r, s, height - r * 2, color);
            quad(x + width - s, y + r, s, height - r * 2, color);
            //bottom
            quarterCircleOutline(x + width - r, y + height - r, r, 1, cirDepth, color, s);
            quad(x + r, y + height - s, width - r * 2, s, color);
            quarterCircleOutline(x + r, y + height - r, r, 2, cirDepth, color, s);
        }
    }

    public void quadRounded(double x, double y, double width, double height, Color color, int r, boolean roundTop) {
        r = getR(r, width, height);
        if (r == 0)
            quad(x, y, width, height, color);
        else {
            int cirDepth = getCir(r);
            if (roundTop) {
                //top
                quarterCircle(x + r, y + r, r, 3, cirDepth, color);
                quad(x + r, y, width - 2 * r, r, color);
                quarterCircle(x + width - r, y + r, r, 0, cirDepth, color);
                //middle
                quad(x, y + r, width, height - 2 * r, color);
            }
            else {
                //middle
                quad(x, y, width, height - r, color);
            }
            //bottom
            quarterCircle(x + width - r, y + height - r, r, 1, cirDepth, color);
            quad(x + r, y + height - r, width - 2 * r, r, color);
            quarterCircle(x + r, y + height - r, r, 2, cirDepth, color);
        }
    }

    public void quadRoundedSide(double x, double y, double width, double height, Color color, int r, boolean right) {
        r = getR(r, width, height);
        if (r == 0)
            quad(x, y, width, height, color);
        else {
            int cirDepth = getCir(r);
            if (right) {
                quarterCircle(x + width - r, y + r, r, 0, cirDepth, color);
                quarterCircle(x + width - r, y + height - r, r, 1, cirDepth, color);
                quad(x, y, width - r, height, color);
                quad(x + width - r, y + r, r, height - r * 2, color);
            }
            else {
                quarterCircle(x + r, y + r, r, 3, cirDepth, color);
                quarterCircle(x + r, y + height - r, r, 2, cirDepth, color);
                quad(x + r, y, width - r, height, color);
                quad(x, y + r, r, height - r * 2, color);
            }
        }
    }

    private int getR(int r, double w, double h) {
        if (r * 2 > h) {
            r = (int)h / 2;
        }
        if (r * 2 > w) {
            r = (int)w / 2;
        }
        return r;
    }

    private int getCir(int r) {
        return Math.max(r / 2, 1);
    }

    private void quarterCircle(double x, double y, double r, double a, int cirDepth, Color color) {
        a *= Math.PI / 2;
        double cirPart = Math.PI / 2 / cirDepth;
        vert2(x + Math.sin(a) * r, y - Math.cos(a) * r, color);
        for (int i = 1; i < cirDepth + 1; i++) {
            vert2(x, y, color);
            double xV = x + Math.sin(a + cirPart * i) * r;
            double yV = y - Math.cos(a + cirPart * i) * r;
            vert2(xV, yV, color);
            if (i != cirDepth)
                vert2(xV, yV, color);
        }
    }

    private void quarterCircleOutline(double x, double y, double r, double a, int cirDepth, Color color, double s) {
        a *= Math.PI / 2;
        double cirPart = Math.PI / 2 / cirDepth;
        for (int i = 0; i < cirDepth; i++) {
            double xOC = x + Math.sin(a + cirPart * i) * r;
            double yOC = y - Math.cos(a + cirPart * i) * r;
            double xIC = x + Math.sin(a + cirPart * i) * (r - s);
            double yIC = y - Math.cos(a + cirPart * i) * (r - s);
            double xON = x + Math.sin(a + cirPart * (i + 1)) * r;
            double yON = y - Math.cos(a + cirPart * (i + 1)) * r;
            double xIN = x + Math.sin(a + cirPart * (i + 1)) * (r - s);
            double yIN = y - Math.cos(a + cirPart * (i + 1)) * (r - s);
            //
            vert2(xOC, yOC, color);
            vert2(xON, yON, color);
            vert2(xIC, yIC, color);
            //
            vert2(xIC, yIC, color);
            vert2(xON, yON, color);
            vert2(xIN, yIN, color);
        }
    }

    public void vert2(double x, double y, Color c) {
        pos(x, y, 0).color(c).endVertex();
    }

    // LINES

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color startColor, Color endColor) {
        pos(x1, y1, z1).color(startColor).endVertex();
        pos(x2, y2, z2).color(endColor).endVertex();
    }

    public void line(double x1, double y1, double x2, double y2, Color startColor, Color endColor) {
        line(x1, y1, 0, x2, y2, 0, startColor, endColor);
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        line(x1, y1, z1, x2, y2, z2, color, color);
    }

    public void line(double x1, double y1, double x2, double y2, Color color) {
        line(x1, y1, 0, x2, y2, 0, color);
    }

    public void boxEdges(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        if (Dir.is(excludeDir, Dir.WEST) && Dir.is(excludeDir, Dir.NORTH)) line(x1, y1, z1, x1, y2, z1, color);
        if (Dir.is(excludeDir, Dir.WEST) && Dir.is(excludeDir, Dir.SOUTH)) line(x1, y1, z2, x1, y2, z2, color);
        if (Dir.is(excludeDir, Dir.EAST) && Dir.is(excludeDir, Dir.NORTH)) line(x2, y1, z1, x2, y2, z1, color);
        if (Dir.is(excludeDir, Dir.EAST) && Dir.is(excludeDir, Dir.SOUTH)) line(x2, y1, z2, x2, y2, z2, color);

        if (Dir.is(excludeDir, Dir.NORTH)) line(x1, y1, z1, x2, y1, z1, color);
        if (Dir.is(excludeDir, Dir.NORTH)) line(x1, y2, z1, x2, y2, z1, color);
        if (Dir.is(excludeDir, Dir.SOUTH)) line(x1, y1, z2, x2, y1, z2, color);
        if (Dir.is(excludeDir, Dir.SOUTH)) line(x1, y2, z2, x2, y2, z2, color);

        if (Dir.is(excludeDir, Dir.WEST)) line(x1, y1, z1, x1, y1, z2, color);
        if (Dir.is(excludeDir, Dir.WEST)) line(x1, y2, z1, x1, y2, z2, color);
        if (Dir.is(excludeDir, Dir.EAST)) line(x2, y1, z1, x2, y1, z2, color);
        if (Dir.is(excludeDir, Dir.EAST)) line(x2, y2, z1, x2, y2, z2, color);
    }

    public void boxEdges(double x, double y, double width, double height, Color color) {
        boxEdges(x, y, 0, x + width, y + height, 0, color, 0);
    }
}
