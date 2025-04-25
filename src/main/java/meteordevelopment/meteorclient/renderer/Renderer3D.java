/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class Renderer3D {
    public final MeshBuilder lines = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_LINES);
    public final MeshBuilder triangles = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED);
    public final MeshBuilder linesDepth = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH);
    public final MeshBuilder trianglesDepth = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED_DEPTH);

    public void begin() {
        lines.begin();
        triangles.begin();
        linesDepth.begin();
        trianglesDepth.begin();
    }

    public void render(MatrixStack matrices) {
        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(MeteorRenderPipelines.WORLD_COLORED_LINES)
            .mesh(lines, matrices)
            .end();

        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(MeteorRenderPipelines.WORLD_COLORED)
            .mesh(triangles, matrices)
            .end();

        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH)
            .mesh(linesDepth, matrices)
            .end();

        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(MeteorRenderPipelines.WORLD_COLORED_DEPTH)
            .mesh(trianglesDepth, matrices)
            .end();
    }

    // Lines

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2, boolean depth) {
        MeshBuilder buffers = depth ? linesDepth : lines;

        buffers.ensureLineCapacity();

        buffers.line(
            buffers.vec3(x1, y1, z1).color(color1).next(),
            buffers.vec3(x2, y2, z2).color(color2).next()
        );
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        line(x1, y1, z1, x2, y2, z2, color1, color2, false);
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color, boolean depth) {
        line(x1, y1, z1, x2, y2, z2, color, color, depth);
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        line(x1, y1, z1, x2, y2, z2, color, color);
    }

    public void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        internalBoxLines(lines, x1, y1, z1, x2, y2, z2, color, excludeDir);
    }

    public void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir, boolean depth) {
        internalBoxLines(depth ? linesDepth : lines, x1, y1, z1, x2, y2, z2, color, excludeDir);
    }

    public void blockLines(int x, int y, int z, Color color, int excludeDir) {
        boxLines(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    // Quads

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft, boolean depth) {
        MeshBuilder buffers = depth ? trianglesDepth : triangles;

        buffers.ensureQuadCapacity();

        buffers.quad(
            buffers.vec3(x1, y1, z1).color(bottomLeft).next(),
            buffers.vec3(x2, y2, z2).color(topLeft).next(),
            buffers.vec3(x3, y3, z3).color(topRight).next(),
            buffers.vec3(x4, y4, z4).color(bottomRight).next()
        );
    }

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        quad(x1, y1 ,z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, topLeft, topRight, bottomRight, bottomLeft, false);
    }

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color, boolean depth) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, color, color, color, depth);
    }

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, color, color, color);
    }

    public void quadVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    public void quadHorizontal(double x1, double y, double z1, double x2, double z2, Color color) {
        quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color);
    }

    public void gradientQuadVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color topColor, Color bottomColor) {
        quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, topColor, topColor, bottomColor, bottomColor);
    }

    // Sides

    @SuppressWarnings("Duplicates")
    public void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color sideColor, Color lineColor, ShapeMode mode) {
        if (mode.lines()) {
            lines.ensureCapacity(4, 8);

            int i1 = lines.vec3(x1, y1, z1).color(lineColor).next();
            int i2 = lines.vec3(x2, y2, z2).color(lineColor).next();
            int i3 = lines.vec3(x3, y3, z3).color(lineColor).next();
            int i4 = lines.vec3(x4, y4, z4).color(lineColor).next();

            lines.line(i1, i2);
            lines.line(i2, i3);
            lines.line(i3, i4);
            lines.line(i4, i1);
        }

        if (mode.sides()) {
            quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, sideColor);
        }
    }

    public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode) {
        side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, sideColor, lineColor, mode);
    }

    public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Color sideColor, Color lineColor, ShapeMode mode) {
        side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, sideColor, lineColor, mode);
    }

    // Boxes

    public void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        internalBoxSides(triangles, x1, y1, z1, x2, y2, z2, color, excludeDir);
    }

    public void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir, boolean depth) {
        internalBoxSides(depth ? trianglesDepth : triangles, x1, y1, z1, x2, y2, z2, color, excludeDir);
    }

    public void blockSides(int x, int y, int z, Color color, int excludeDir) {
        boxSides(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    public void box(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
        if (mode.sides()) boxSides(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
    }

    public void box(BlockPos pos, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, lineColor, excludeDir);
        if (mode.sides()) boxSides(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor, excludeDir);
    }

    public void box(Box box, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, lineColor, excludeDir);
        if (mode.sides()) boxSides(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sideColor, excludeDir);
    }

    // Internal

    @SuppressWarnings("Duplicates")
    private void internalBoxLines(MeshBuilder buffers, double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        buffers.ensureCapacity(8, 24);

        int blb = buffers.vec3(x1, y1, z1).color(color).next();
        int blf = buffers.vec3(x1, y1, z2).color(color).next();
        int brb = buffers.vec3(x2, y1, z1).color(color).next();
        int brf = buffers.vec3(x2, y1, z2).color(color).next();
        int tlb = buffers.vec3(x1, y2, z1).color(color).next();
        int tlf = buffers.vec3(x1, y2, z2).color(color).next();
        int trb = buffers.vec3(x2, y2, z1).color(color).next();
        int trf = buffers.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            buffers.line(blb, tlb);
            buffers.line(blf, tlf);
            buffers.line(brb, trb);
            buffers.line(brf, trf);

            // Bottom loop
            buffers.line(blb, blf);
            buffers.line(brb, brf);
            buffers.line(blb, brb);
            buffers.line(blf, brf);

            // Top loop
            buffers.line(tlb, tlf);
            buffers.line(trb, trf);
            buffers.line(tlb, trb);
            buffers.line(tlf, trf);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.NORTH)) buffers.line(blb, tlb);
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.SOUTH)) buffers.line(blf, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.NORTH)) buffers.line(brb, trb);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.SOUTH)) buffers.line(brf, trf);

            // Bottom loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.DOWN)) buffers.line(blb, blf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.DOWN)) buffers.line(brb, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.DOWN)) buffers.line(blb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.DOWN)) buffers.line(blf, brf);

            // Top loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.UP)) buffers.line(tlb, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.UP)) buffers.line(trb, trf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.UP)) buffers.line(tlb, trb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.UP)) buffers.line(tlf, trf);
        }
    }

    @SuppressWarnings("Duplicates")
    private void internalBoxSides(MeshBuilder buffers, double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        buffers.ensureCapacity(8, 36);

        int blb = buffers.vec3(x1, y1, z1).color(color).next();
        int blf = buffers.vec3(x1, y1, z2).color(color).next();
        int brb = buffers.vec3(x2, y1, z1).color(color).next();
        int brf = buffers.vec3(x2, y1, z2).color(color).next();
        int tlb = buffers.vec3(x1, y2, z1).color(color).next();
        int tlf = buffers.vec3(x1, y2, z2).color(color).next();
        int trb = buffers.vec3(x2, y2, z1).color(color).next();
        int trf = buffers.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            buffers.quad(blb, blf, tlf, tlb);
            buffers.quad(brb, trb, trf, brf);
            buffers.quad(blb, tlb, trb, brb);
            buffers.quad(blf, brf, trf, tlf);

            // Bottom
            buffers.quad(blb, brb, brf, blf);

            // Top
            buffers.quad(tlb, tlf, trf, trb);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST)) buffers.quad(blb, blf, tlf, tlb);
            if (Dir.isNot(excludeDir, Dir.EAST)) buffers.quad(brb, trb, trf, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH)) buffers.quad(blb, tlb, trb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH)) buffers.quad(blf, brf, trf, tlf);

            // Bottom
            if (Dir.isNot(excludeDir, Dir.DOWN)) buffers.quad(blb, brb, brf, blf);

            // Top
            if (Dir.isNot(excludeDir, Dir.UP)) buffers.quad(tlb, tlf, trf, trb);
        }
    }
}
