/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.renderer;

import minegame159.meteorclient.gui.renderer.packer.TextureRegion;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

public class Renderer2D {
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;

    public final Mesh triangles;
    public final Mesh lines;

    public Renderer2D(boolean texture) {
        triangles = new ShaderMesh(
            texture ? Shaders.POS_TEX_COLOR : Shaders.POS_COLOR,
            DrawMode.Triangles,
            texture ? new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color} : new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Color}
        );

        lines = new ShaderMesh(Shaders.POS_COLOR, DrawMode.Lines, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
    }

    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
    }

    public void setAlpha(double alpha) {
        triangles.alpha = alpha;
    }

    public void begin() {
        triangles.begin();
        lines.begin();
    }

    public void end() {
        triangles.end();
        lines.end();
    }

    public void render(MatrixStack matrices) {
        triangles.render(matrices, false);
        lines.render(matrices, false);
    }

    // Lines

    public void line(double x1, double y1, double x2, double y2, Color color) {
        lines.line(
            lines.vec2(x1, y1).color(color).next(),
            lines.vec2(x2, y2).color(color).next()
        );
    }

    public void boxLines(double x, double y, double width, double height, Color color) {
        int i1 = lines.vec2(x, y).color(color).next();
        int i2 = lines.vec2(x, y + height).color(color).next();
        int i3 = lines.vec2(x + width, y + height).color(color).next();
        int i4 = lines.vec2(x + width, y).color(color).next();

        lines.line(i1, i2);
        lines.line(i2, i3);
        lines.line(i3, i4);
        lines.line(i4, i1);
    }

    // Quads

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        triangles.quad(
            triangles.vec2(x, y).color(cTopLeft).next(),
            triangles.vec2(x, y + height).color(cBottomLeft).next(),
            triangles.vec2(x + width, y + height).color(cBottomRight).next(),
            triangles.vec2(x + width, y).color(cTopRight).next()
        );
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color);
    }

    // Textured quads

    public void texQuad(double x, double y, double width, double height, Color color) {
        triangles.quad(
            triangles.vec2(x, y).vec2(0, 0).color(color).next(),
            triangles.vec2(x, y + height).vec2(0, 1).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(1, 1).color(color).next(),
            triangles.vec2(x + width, y).vec2(1, 0).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        triangles.quad(
            triangles.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
            triangles.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
            triangles.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, double rotation, double texX1, double texY1, double texX2, double texY2, Color color) {
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double oX = x + width / 2;
        double oY = y + height / 2;

        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        int i1 = triangles.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();

        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = triangles.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();

        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = triangles.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();

        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = triangles.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();

        triangles.quad(i1, i2, i3, i4);
    }

    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }

    // Rounded quad

    private final double circleNone = 0;
    private final double circleQuarter = Math.PI / 2;
    private final double circleHalf = circleQuarter * 2;
    private final double circleThreeQuarter = circleQuarter * 3;

    public void quadRoundedOutline(double x, double y, double width, double height, Color color, int r, double s) {
        r = getR(r, width, height);
        if (r == 0) {
            quad(x, y, width, s, color);
            quad(x, y + height - s, width, s, color);
            quad(x, y + s, s, height - s * 2, color);
            quad(x + width - s, y + s, s, height - s * 2, color);
        }
        else {
            //top
            circlePartOutline(x + r, y + r, r, circleThreeQuarter, circleQuarter, color, s);
            quad(x + r, y, width - r * 2, s, color);
            circlePartOutline(x + width - r, y + r, r, circleNone, circleQuarter, color, s);
            //middle
            quad(x, y + r, s, height - r * 2, color);
            quad(x + width - s, y + r, s, height - r * 2, color);
            //bottom
            circlePartOutline(x + width - r, y + height - r, r, circleQuarter, circleQuarter, color, s);
            quad(x + r, y + height - s, width - r * 2, s, color);
            circlePartOutline(x + r, y + height - r, r, circleHalf, circleQuarter, color, s);
        }
    }

    public void quadRounded(double x, double y, double width, double height, Color color, int r, boolean roundTop) {
        r = getR(r, width, height);
        if (r == 0)
            quad(x, y, width, height, color);
        else {
            if (roundTop) {
                //top
                circlePart(x + r, y + r, r, circleThreeQuarter, circleQuarter, color);
                quad(x + r, y, width - 2 * r, r, color);
                circlePart(x + width - r, y + r, r, circleNone, circleQuarter, color);
                //middle
                quad(x, y + r, width, height - 2 * r, color);
            }
            else {
                //middle
                quad(x, y, width, height - r, color);
            }
            //bottom
            circlePart(x + width - r, y + height - r, r, circleQuarter, circleQuarter, color);
            quad(x + r, y + height - r, width - 2 * r, r, color);
            circlePart(x + r, y + height - r, r, circleHalf, circleQuarter, color);
        }
    }

    public void quadRoundedSide(double x, double y, double width, double height, Color color, int r, boolean right) {
        r = getR(r, width, height);
        if (r == 0)
            quad(x, y, width, height, color);
        else {
            if (right) {
                circlePart(x + width - r, y + r, r, circleNone, circleQuarter, color);
                circlePart(x + width - r, y + height - r, r, circleQuarter, circleQuarter, color);
                quad(x, y, width - r, height, color);
                quad(x + width - r, y + r, r, height - r * 2, color);
            }
            else {
                circlePart(x + r, y + r, r, circleThreeQuarter, circleQuarter, color);
                circlePart(x + r, y + height - r, r, circleHalf, circleQuarter, color);
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

    private int getCirDepth(double r, double angle) {
        return Math.max(1, (int)(angle * r / circleQuarter));
    }

    public void circlePart(double x, double y, double r, double startAngle, double angle, Color color) {
        int cirDepth = getCirDepth(r, angle);
        double cirPart = angle / cirDepth;
        int center = triangles.vec2(x, y).color(color).next();
        int prev = triangles.vec2(x + Math.sin(startAngle) * r, y - Math.cos(startAngle) * r).color(color).next();
        for (int i = 1; i < cirDepth + 1; i++) {
            double xV = x + Math.sin(startAngle + cirPart * i) * r;
            double yV = y - Math.cos(startAngle + cirPart * i) * r;
            int next = triangles.vec2(xV, yV).color(color).next();
            triangles.triangle(prev, center, next);
            prev = next;
        }
    }

    public void circlePartOutline(double x, double y, double r, double startAngle, double angle, Color color, double outlineWidth) {
        int cirDepth = getCirDepth(r, angle);
        double cirPart = angle / cirDepth;
        for (int i = 0; i < cirDepth; i++) {
            double xOC = x + Math.sin(startAngle + cirPart * i) * r;
            double yOC = y - Math.cos(startAngle + cirPart * i) * r;
            double xIC = x + Math.sin(startAngle + cirPart * i) * (r - outlineWidth);
            double yIC = y - Math.cos(startAngle + cirPart * i) * (r - outlineWidth);
            double xON = x + Math.sin(startAngle + cirPart * (i + 1)) * r;
            double yON = y - Math.cos(startAngle + cirPart * (i + 1)) * r;
            double xIN = x + Math.sin(startAngle + cirPart * (i + 1)) * (r - outlineWidth);
            double yIN = y - Math.cos(startAngle + cirPart * (i + 1)) * (r - outlineWidth);

            triangles.quad(
                triangles.vec2(xOC, yOC).color(color).next(),
                triangles.vec2(xON, yON).color(color).next(),
                triangles.vec2(xIC, yIC).color(color).next(),
                triangles.vec2(xIN, yIN).color(color).next()
            );
        }
    }
}
