/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.TextureRegion;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShapeBuilder {
    private static final MeshBuilder meshBuilder = new MeshBuilder();
    public static MeshBuilder triangles = Renderer.TRIANGLES;

    public static void begin(RenderEvent event, int drawMode, VertexFormat vertexFormat) {
        meshBuilder.begin(event, drawMode, vertexFormat);
        triangles = meshBuilder;
        Matrices.push();
    }

    public static void end(boolean texture) {
        triangles.end(texture);
        triangles = Renderer.TRIANGLES;
        Matrices.pop();
    }
    public static void end() {
        end(false);
    }

    // Quad
    public static void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
        triangles.pos(x1, y1, z1).color(color).endVertex();
        triangles.pos(x2, y2, z2).color(color).endVertex();
        triangles.pos(x3, y3, z3).color(color).endVertex();

        triangles.pos(x1, y1, z1).color(color).endVertex();
        triangles.pos(x3, y3, z3).color(color).endVertex();
        triangles.pos(x4, y4, z4).color(color).endVertex();
    }

    public static void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, color);
    }

    public static void quad(double x, double y, double width, double height, double rotation, Color color) {
        double cos = Math.cos(Math.toRadians(rotation));
        double sin = Math.sin(Math.toRadians(rotation));

        double oX = x + width / 2;
        double oY = y + height / 2;

        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;

        double _x2 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y2 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;

        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;

        double _x4 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y4 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;

        quad(_x1, _y1, 0, _x2, _y2, 0, _x3, _y3, 0, _x4, _y4, 0, color);
    }

    public static void texQuad(double x, double y, double width, double height, double srcX, double srcY, double srcWidth, double srcHeight, Color color1, Color color2, Color color3, Color color4) {
        triangles.pos(x, y, 0).texture(srcX, srcY).color(color1).endVertex();
        triangles.pos(x + width, y, 0).texture(srcX + srcWidth, srcY).color(color2).endVertex();
        triangles.pos(x + width, y + height, 0).texture(srcX + srcWidth, srcY + srcHeight).color(color3).endVertex();

        triangles.pos(x, y, 0).texture(srcX, srcY).color(color1).endVertex();
        triangles.pos(x + width, y + height, 0).texture(srcX + srcWidth, srcY + srcHeight).color(color3).endVertex();
        triangles.pos(x, y + height, 0).texture(srcX, srcY + srcHeight).color(color4).endVertex();
    }
    public static void texQuad(double x, double y, double width, double height, TextureRegion tex, Color color1, Color color2, Color color3, Color color4) {
        texQuad(x, y, width, height, tex.x, tex.y, tex.width, tex.height, color1, color2, color3, color4);
    }

    // Triangle
    public static void triangle(double x, double y, double size, double angle, Color color) {
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));

        double oX = x + size / 2;
        double oY = y + size / 4;

        double _x = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        triangles.pos(_x, _y, 0).color(color).endVertex();

        _x = ((x + size - oX) * cos) - ((y - oY) * sin) + oX;
        _y = ((y - oY) * cos) + ((x + size - oX) * sin) + oY;
        triangles.pos(_x, _y, 0).color(color).endVertex();

        double v = y + size / 0.9 - oY;
        _x = ((x + size / 2 - oX) * cos) - (v * sin) + oX;
        _y = (v * cos) + ((x + size / 2 - oX) * sin) + oY;
        triangles.pos(_x, _y, 0).color(color).endVertex();
    }

    // Line
    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        Renderer.LINES.pos(x1, y1, z1).color(color).endVertex();
        Renderer.LINES.pos(x2, y2, z2).color(color).endVertex();
    }
    public static void line(double x1, double y1, double x2, double y2, Color color) {
        line(x1, y1, 0, x2, y2, 0, color);
    }

    // Box edges
    public static void boxEdges(double x1, double y1, double z1, double x2, double y2, double z2, Color color, Direction excludeDir) {
        if (excludeDir != Direction.WEST && excludeDir != Direction.NORTH) line(x1, y1, z1, x1, y2, z1, color);
        if (excludeDir != Direction.WEST && excludeDir != Direction.SOUTH) line(x1, y1, z2, x1, y2, z2, color);
        if (excludeDir != Direction.EAST && excludeDir != Direction.NORTH) line(x2, y1, z1, x2, y2, z1, color);
        if (excludeDir != Direction.EAST && excludeDir != Direction.SOUTH) line(x2, y1, z2, x2, y2, z2, color);

        if (excludeDir != Direction.NORTH) line(x1, y1, z1, x2, y1, z1, color);
        if (excludeDir != Direction.NORTH) line(x1, y2, z1, x2, y2, z1, color);
        if (excludeDir != Direction.SOUTH) line(x1, y1, z2, x2, y1, z2, color);
        if (excludeDir != Direction.SOUTH) line(x1, y2, z2, x2, y2, z2, color);

        if (excludeDir != Direction.WEST) line(x1, y1, z1, x1, y1, z2, color);
        if (excludeDir != Direction.WEST) line(x1, y2, z1, x1, y2, z2, color);
        if (excludeDir != Direction.EAST) line(x2, y1, z1, x2, y1, z2, color);
        if (excludeDir != Direction.EAST) line(x2, y2, z1, x2, y2, z2, color);
    }
    public static void boxEdges(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        boxEdges(x1, y1, z1, x2, y2, z2, color, null);
    }

    public static void blockEdges(int x, int y, int z, Color color, Direction excludeDir) {
        boxEdges(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }
    public static void blockEdges(BlockPos blockPos, Color color) {
        blockEdges(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color, null);
    }

    // Box sides
    public static void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, Direction excludeDir) {
        if (excludeDir != Direction.DOWN) quad(x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, color); // Bottom
        if (excludeDir != Direction.UP) quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color); // Top

        if (excludeDir != Direction.NORTH) quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color); // Front
        if (excludeDir != Direction.SOUTH) quad(x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, color); // Back

        if (excludeDir != Direction.WEST) quad(x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, color); // Left
        if (excludeDir != Direction.EAST) quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color); // Right
    }
    public static void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        boxSides(x1, y1, z1, x2, y2, z2, color, null);
    }

    public static void blockSides(int x, int y, int z, Color color, Direction excludeDir) {
        boxSides(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    // Other
    public static void quadWithLinesVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor) {
        quad(x1, y1, z1, x2, y1, z2, x2, y2, z2, x1, y2, z1, sideColor);
        line(x1, y1, z1, x2, y1, z2, lineColor);
        line(x1, y1, z1, x1, y2, z1, lineColor);
        line(x1, y2, z1, x2, y2, z2, lineColor);
        line(x2, y1, z2, x2, y2, z2, lineColor);
    }
    public static void quadWithLines(double x, double y, double z, double width, double height, Color sideColor, Color lineColor) {
        quad(x, y, z, x, y, z + height, x + width, y, z + height, x + width, y, z, sideColor);
        line(x, y, z, x, y, z + height, lineColor);
        line(x, y, z + height, x + width, y, z + height, lineColor);
        line(x + width, y, z + height, x + width, y, z, lineColor);
        line(x, y, z, x + width, y, z, lineColor);
    }
    public static void quadWithLines(double x, double y, double z, Color sideColor, Color lineColor) {
        quadWithLines(x, y, z, 1, 1, sideColor, lineColor);
    }
    public static void emptyQuadWithLines(double x, double y, double z, double width, double height, Color lineColor) {
        line(x, y, z, x, y, z + height, lineColor);
        line(x, y, z + height, x + width, y, z + height, lineColor);
        line(x + width, y, z + height, x + width, y, z, lineColor);
        line(x, y, z, x + width, y, z, lineColor);
    }
    public static void emptyQuadWithLines(double x, double y, double z, Color lineColor) {
        emptyQuadWithLines(x, y, z, 1, 1, lineColor);
    }
}
