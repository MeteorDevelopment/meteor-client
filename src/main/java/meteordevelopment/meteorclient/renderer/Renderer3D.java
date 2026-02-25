/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class Renderer3D {
    public final MeshBuilder lines;
    public final MeshBuilder triangles;
    private final RenderPipeline linesPipeline;
    private final RenderPipeline trianglesPipeline;

    public Renderer3D(RenderPipeline lines, RenderPipeline triangles) {
        this.lines = new MeshBuilder(lines);
        this.triangles = new MeshBuilder(triangles);
        this.linesPipeline = lines;
        this.trianglesPipeline = triangles;
    }

    public void begin() {
        lines.begin();
        triangles.begin();
    }

    public void render(MatrixStack matrices) {
        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(linesPipeline)
            .mesh(lines, matrices)
            .end();

        MeshRenderer.begin()
            .attachments(MinecraftClient.getInstance().getFramebuffer())
            .pipeline(trianglesPipeline)
            .mesh(triangles, matrices)
            .end();
    }

    // Lines

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        line(x1, y1, z1, x2, y2, z2, color, color);
    }

    public void thickLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, double width) {
        thickLine(x1, y1, z1, x2, y2, z2, color, color, width);
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        double[] p = reprojectBehindCamera(x1, y1, z1, x2, y2, z2);

        lines.ensureLineCapacity();
        lines.line(
            lines.vec3(p[0], p[1], p[2]).color(color1).next(),
            lines.vec3(p[3], p[4], p[5]).color(color2).next()
        );
    }

    public void thickLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2, double width) {
        width = sliderToLinearWidth(width);
        if (width <= 0) {
            line(x1, y1, z1, x2, y2, z2, color1, color2);
            return;
        }

        double[] p = reprojectBehindCamera(x1, y1, z1, x2, y2, z2);
        x1 = p[0]; y1 = p[1]; z1 = p[2];
        x2 = p[3]; y2 = p[4]; z2 = p[5];

        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        var cameraPos = camera.getCameraPos();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        // Camera forward direction
        double yawRad = Math.toRadians(camera.getYaw());
        double pitchRad = Math.toRadians(camera.getPitch());
        double fwdX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fwdY = -Math.sin(pitchRad);
        double fwdZ =  Math.cos(yawRad) * Math.cos(pitchRad);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return;

        double lineDirX = dx / length;
        double lineDirY = dy / length;
        double lineDirZ = dz / length;

        // Perpendicular = cross(lineDir, cameraForward)
        // This always lies in the camera's view plane, so basically you always see a rectangle
        double perpX = lineDirY * fwdZ - lineDirZ * fwdY;
        double perpY = lineDirZ * fwdX - lineDirX * fwdZ;
        double perpZ = lineDirX * fwdY - lineDirY * fwdX;

        double perpLen = Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);

        if (perpLen < 1e-6) {
            // Line is parallel to camera forward - use camera right vector as fallback
            perpX = Math.cos(yawRad);
            perpY = 0;
            perpZ = Math.sin(yawRad);
            perpLen = Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
            if (perpLen < 1e-6) return;
        }

        perpX /= perpLen;
        perpY /= perpLen;
        perpZ /= perpLen;

        double dist1 = Math.sqrt((x1 - camX) * (x1 - camX) + (y1 - camY) * (y1 - camY) + (z1 - camZ) * (z1 - camZ));
        double dist2 = Math.sqrt((x2 - camX) * (x2 - camX) + (y2 - camY) * (y2 - camY) + (z2 - camZ) * (z2 - camZ));

        double hw1 = width * dist1;
        double hw2 = width * dist2;

        triangles.ensureCapacity(4, 6);

        quad(
            x1 - perpX * hw1, y1 - perpY * hw1, z1 - perpZ * hw1,
            x1 + perpX * hw1, y1 + perpY * hw1, z1 + perpZ * hw1,
            x2 + perpX * hw2, y2 + perpY * hw2, z2 + perpZ * hw2,
            x2 - perpX * hw2, y2 - perpY * hw2, z2 - perpZ * hw2,
            color1, color1, color2, color2
        );
    }

    private double[] reprojectBehindCamera(double x1, double y1, double z1, double x2, double y2, double z2) {
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        var cameraPos = camera.getCameraPos();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        double yawRad = Math.toRadians(camera.getYaw());
        double pitchRad = Math.toRadians(camera.getPitch());
        double fwdX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fwdY = -Math.sin(pitchRad);
        double fwdZ =  Math.cos(yawRad) * Math.cos(pitchRad);

        double[] result = { x1, y1, z1, x2, y2, z2 };

        for (int i = 0; i < 2; i++) {
            double px = result[i * 3]     - camX;
            double py = result[i * 3 + 1] - camY;
            double pz = result[i * 3 + 2] - camZ;
            double dot = px * fwdX + py * fwdY + pz * fwdZ;

            if (dot >= 0.05) continue;

            double latX = px - fwdX * dot;
            double latY = py - fwdY * dot;
            double latZ = pz - fwdZ * dot;
            double latLen = Math.sqrt(latX * latX + latY * latY + latZ * latZ);

            if (latLen < 1e-6) {
                if (Math.abs(fwdY) < 0.9) {
                    latX = fwdZ;
                    latY = 0;
                    latZ = -fwdX;
                } else {
                    latX = 1;
                    latY = 0;
                    latZ = 0;
                }
                latLen = Math.sqrt(latX * latX + latY * latY + latZ * latZ);
            }

            latX /= latLen;
            latY /= latLen;
            latZ /= latLen;

            double pushDist = 1000.0;
            result[i * 3]     = camX + fwdX * 0.1 + latX * pushDist;
            result[i * 3 + 1] = camY + fwdY * 0.1 + latY * pushDist;
            result[i * 3 + 2] = camZ + fwdZ * 0.1 + latZ * pushDist;
        }

        return result;
    }

    @SuppressWarnings("Duplicates")
    public void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        lines.ensureCapacity(8, 24);

        int blb = lines.vec3(x1, y1, z1).color(color).next();
        int blf = lines.vec3(x1, y1, z2).color(color).next();
        int brb = lines.vec3(x2, y1, z1).color(color).next();
        int brf = lines.vec3(x2, y1, z2).color(color).next();
        int tlb = lines.vec3(x1, y2, z1).color(color).next();
        int tlf = lines.vec3(x1, y2, z2).color(color).next();
        int trb = lines.vec3(x2, y2, z1).color(color).next();
        int trf = lines.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            lines.line(blb, tlb);
            lines.line(blf, tlf);
            lines.line(brb, trb);
            lines.line(brf, trf);

            // Bottom loop
            lines.line(blb, blf);
            lines.line(brb, brf);
            lines.line(blb, brb);
            lines.line(blf, brf);

            // Top loop
            lines.line(tlb, tlf);
            lines.line(trb, trf);
            lines.line(tlb, trb);
            lines.line(tlf, trf);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(blb, tlb);
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(blf, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(brb, trb);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(brf, trf);

            // Bottom loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, blf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(brb, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blf, brf);

            // Top loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(trb, trf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, trb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlf, trf);
        }
    }

    public void blockLines(int x, int y, int z, Color color, int excludeDir) {
        boxLines(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    // Quads

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        triangles.ensureQuadCapacity();

        triangles.quad(
            triangles.vec3(x1, y1, z1).color(bottomLeft).next(),
            triangles.vec3(x2, y2, z2).color(topLeft).next(),
            triangles.vec3(x3, y3, z3).color(topRight).next(),
            triangles.vec3(x4, y4, z4).color(bottomRight).next()
        );
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

    @SuppressWarnings("Duplicates")
    public void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        triangles.ensureCapacity(8, 36);

        int blb = triangles.vec3(x1, y1, z1).color(color).next();
        int blf = triangles.vec3(x1, y1, z2).color(color).next();
        int brb = triangles.vec3(x2, y1, z1).color(color).next();
        int brf = triangles.vec3(x2, y1, z2).color(color).next();
        int tlb = triangles.vec3(x1, y2, z1).color(color).next();
        int tlf = triangles.vec3(x1, y2, z2).color(color).next();
        int trb = triangles.vec3(x2, y2, z1).color(color).next();
        int trf = triangles.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            triangles.quad(blb, blf, tlf, tlb);
            triangles.quad(brb, trb, trf, brf);
            triangles.quad(blb, tlb, trb, brb);
            triangles.quad(blf, brf, trf, tlf);

            // Bottom
            triangles.quad(blb, brb, brf, blf);

            // Top
            triangles.quad(tlb, tlf, trf, trb);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST)) triangles.quad(blb, blf, tlf, tlb);
            if (Dir.isNot(excludeDir, Dir.EAST)) triangles.quad(brb, trb, trf, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH)) triangles.quad(blb, tlb, trb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH)) triangles.quad(blf, brf, trf, tlf);

            // Bottom
            if (Dir.isNot(excludeDir, Dir.DOWN)) triangles.quad(blb, brb, brf, blf);

            // Top
            if (Dir.isNot(excludeDir, Dir.UP)) triangles.quad(tlb, tlf, trf, trb);
        }
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

    private double sliderToLinearWidth(double sliderValue) {
        if (sliderValue <= 0) return 0;
        return 0.001 + (sliderValue / 10.0) * (0.01 - 0.001);
    }
}
