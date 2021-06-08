/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.renderer;

import minegame159.meteorclient.gui.renderer.packer.TextureRegion;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

public class Renderer2D {
    public final Mesh mesh;

    public Renderer2D(boolean texture) {
        mesh = new ShaderMesh(
            texture ? Shaders.POS_TEX_COLOR : Shaders.POS_COLOR,
            DrawMode.Triangles,
            texture ? new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color} : new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Color}
        );
    }

    public void setAlpha(double alpha) {
        mesh.alpha = alpha;
    }

    public void begin() {
        mesh.begin();
    }

    public void end() {
        mesh.end();
    }

    public void render(MatrixStack matrices) {
        mesh.render(matrices);
    }

    // Quads

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        mesh.quad(
            mesh.vec2(x, y).color(cTopLeft).next(),
            mesh.vec2(x, y + height).color(cBottomLeft).next(),
            mesh.vec2(x + width, y + height).color(cBottomRight).next(),
            mesh.vec2(x + width, y).color(cTopRight).next()
        );
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color);
    }

    // Textured quads

    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        mesh.quad(
            mesh.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
            mesh.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
            mesh.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
            mesh.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
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
        int i1 = mesh.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();

        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = mesh.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();

        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = mesh.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();

        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = mesh.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();

        mesh.quad(i1, i2, i3, i4);
    }

    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }
}
