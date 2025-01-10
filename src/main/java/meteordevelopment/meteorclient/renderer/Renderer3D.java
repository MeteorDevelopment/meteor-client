/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.meteordev.juno.api.Device;
import org.meteordev.juno.api.commands.RenderPass;
import org.meteordev.juno.api.pipeline.GraphicsPipeline;
import org.meteordev.juno.api.pipeline.Shader;
import org.meteordev.juno.api.pipeline.ShaderType;
import org.meteordev.juno.api.pipeline.state.*;
import org.meteordev.juno.api.pipeline.vertexformat.StandardFormats;
import org.meteordev.juno.utils.MeshBuilder;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Renderer3D {
    private static final ByteBuffer uniforms = MemoryUtil.memAlloc(4 * 4 * 4);

    private static GraphicsPipeline pipelineLinesNoDepthTest;
    private static GraphicsPipeline pipelineTrianglesNoDepthTest;

    private static GraphicsPipeline pipelineLinesDepthTest;
    private static GraphicsPipeline pipelineTrianglesDepthTest;

    public final MeshBuilder lines = new MeshBuilder(StandardFormats.POSITION_3D_COLOR, PrimitiveType.LINES);
    public final MeshBuilder triangles = new MeshBuilder(StandardFormats.POSITION_3D_COLOR, PrimitiveType.TRIANGLES);

    public boolean depthTest = false;

    private double offsetX;
    private double offsetY;
    private double offsetZ;

    public void begin() {
        lines.begin();
        triangles.begin();

        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        offsetX = camera.x;
        offsetY = camera.y;
        offsetZ = camera.z;
    }

    public void end() {
        lines.end();
        triangles.end();
    }

    public void draw(RenderPass pass, Matrix4f transform) {
        if (pipelineLinesNoDepthTest == null)
            init(pass.getCommandList().getDevice());

        transform.get(uniforms);

        GraphicsPipeline pipelineLines;
        GraphicsPipeline pipelineTriangles;

        if (depthTest) {
            pipelineLines = pipelineLinesDepthTest;
            pipelineTriangles = pipelineTrianglesDepthTest;
        } else {
            pipelineLines = pipelineLinesNoDepthTest;
            pipelineTriangles = pipelineTrianglesNoDepthTest;
        }

        pass.bindPipeline(pipelineLines);
        pass.setUniforms(uniforms, 0);
        lines.draw(pass);

        pass.bindPipeline(pipelineTriangles);
        pass.setUniforms(uniforms, 0);
        triangles.draw(pass);
    }

    public void delete() {
        lines.delete();
        triangles.delete();
    }

    private static void init(Device device) {
        Shader vertex = device.createShader(
            ShaderType.VERTEX,
            """
            #version 330 core

            layout (location = 0) in vec3 pos;
            layout (location = 1) in vec4 color;

            layout (binding = 0) uniform Uniforms {
                mat4 u_Transform;
            };

            out vec4 a_Color;

            void main() {
                gl_Position = u_Transform * vec4(pos, 1.0);
                a_Color = color;
            }
            """,
            "Renderer3D - Vertex"
            );

        Shader fragment = device.createShader(
            ShaderType.FRAGMENT,
            """
            #version 330 core

            layout (location = 0) out vec4 color;

            in vec4 a_Color;

            void main() {
                color = a_Color;
            }
            """,
            "Renderer3D - Fragment"
        );

        pipelineLinesNoDepthTest = device.createGraphicsPipeline(
            new RenderStateBuilder()
                .setVertexFormat(StandardFormats.POSITION_3D_COLOR)
                .setPrimitiveType(PrimitiveType.LINES)
                .setBlendFunc(BlendFunc.alphaBlend())
                .setWriteMask(WriteMask.COLOR)
                .build(),
            vertex,
            fragment,
            "Renderer3D - Lines"
        );

        pipelineTrianglesNoDepthTest = device.createGraphicsPipeline(
            new RenderStateBuilder()
                .setVertexFormat(StandardFormats.POSITION_3D_COLOR)
                .setPrimitiveType(PrimitiveType.TRIANGLES)
                .setBlendFunc(BlendFunc.alphaBlend())
                .setWriteMask(WriteMask.COLOR)
                .build(),
            vertex,
            fragment,
            "Renderer3D - Triangles"
        );

        pipelineLinesDepthTest = device.createGraphicsPipeline(
            new RenderStateBuilder()
                .setVertexFormat(StandardFormats.POSITION_3D_COLOR)
                .setPrimitiveType(PrimitiveType.LINES)
                .setBlendFunc(BlendFunc.alphaBlend())
                .setDepthFunc(DepthFunc.LESS)
                .setWriteMask(WriteMask.COLOR_DEPTH)
                .build(),
            vertex,
            fragment,
            "Renderer3D - Lines"
        );

        pipelineTrianglesDepthTest = device.createGraphicsPipeline(
            new RenderStateBuilder()
                .setVertexFormat(StandardFormats.POSITION_3D_COLOR)
                .setPrimitiveType(PrimitiveType.TRIANGLES)
                .setBlendFunc(BlendFunc.alphaBlend())
                .setDepthFunc(DepthFunc.LESS)
                .setWriteMask(WriteMask.COLOR_DEPTH)
                .build(),
            vertex,
            fragment,
            "Renderer3D - Triangles"
        );

        vertex.invalidate();
        fragment.invalidate();
    }

    // Lines

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        lines.line(
            lineVertex(x1, y1, z1, color1),
            lineVertex(x2, y2, z2, color2)
        );
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        line(x1, y1, z1, x2, y2, z2, color, color);
    }

    @SuppressWarnings("Duplicates")
    public void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        int blb = lineVertex(x1, y1, z1, color);
        int blf = lineVertex(x1, y1, z2, color);
        int brb = lineVertex(x2, y1, z1, color);
        int brf = lineVertex(x2, y1, z2, color);
        int tlb = lineVertex(x1, y2, z1, color);
        int tlf = lineVertex(x1, y2, z2, color);
        int trb = lineVertex(x2, y2, z1, color);
        int trf = lineVertex(x2, y2, z2, color);

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

        lines.growIfNeeded();
    }

    public void blockLines(int x, int y, int z, Color color, int excludeDir) {
        boxLines(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    // Quads

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        triangles.quad(
            triangleVertex(x1, y1, z1, bottomLeft),
            triangleVertex(x2, y2, z2, topLeft),
            triangleVertex(x3, y3, z3, topRight),
            triangleVertex(x4, y4, z4, bottomRight)
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
            int i1 = lineVertex(x1, y1, z1, lineColor);
            int i2 = lineVertex(x2, y2, z2, lineColor);
            int i3 = lineVertex(x3, y3, z3, lineColor);
            int i4 = lineVertex(x4, y4, z4, lineColor);

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
        int blb = triangleVertex(x1, y1, z1, color);
        int blf = triangleVertex(x1, y1, z2, color);
        int brb = triangleVertex(x2, y1, z1, color);
        int brf = triangleVertex(x2, y1, z2, color);
        int tlb = triangleVertex(x1, y2, z1, color);
        int tlf = triangleVertex(x1, y2, z2, color);
        int trb = triangleVertex(x2, y2, z1, color);
        int trf = triangleVertex(x2, y2, z2, color);

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

        triangles.growIfNeeded();
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

    // Utils

    public int lineVertex(double x, double y, double z, Color c) {
        return lines
            .float3((float) (x - offsetX), (float) (y - offsetY), (float) (z - offsetZ))
            .color((byte) c.r, (byte) c.g, (byte) c.b, (byte) c.a)
            .next();
    }

    public int triangleVertex(double x, double y, double z, Color c) {
        return triangles
            .float3((float) (x - offsetX), (float) (y - offsetY), (float) (z - offsetZ))
            .color((byte) c.r, (byte) c.g, (byte) c.b, (byte) c.a)
            .next();
    }
}
