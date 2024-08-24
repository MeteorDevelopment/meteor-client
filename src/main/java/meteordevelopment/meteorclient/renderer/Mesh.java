/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    public enum Attrib {
        Float(1, 4, false),
        Vec2(2, 4, false),
        Vec3(3, 4, false),
        Color(4, 1, true);

        public final int count, size;
        public final boolean normalized;

        Attrib(int count, int componentSize, boolean normalized) {
            this.count = count;
            this.size = count * componentSize;
            this.normalized = normalized;
        }

        public int getType() {
            return this == Color ? GL_UNSIGNED_BYTE : GL_FLOAT;
        }
    }

    public boolean depthTest = false;
    public double alpha = 1;

    private final DrawMode drawMode;
    private final int primitiveVerticesSize;

    private final int vao, vbo, ibo;

    private ByteBuffer vertices;
    private long verticesPointerStart, verticesPointer;

    private ByteBuffer indices;
    private long indicesPointer;

    private int vertexI, indicesCount;

    private boolean building, rendering3D;
    private double cameraX, cameraZ;
    private boolean beganRendering;

    public Mesh(DrawMode drawMode, Attrib... attributes) {
        int stride = 0;
        for (Attrib attribute : attributes) stride += attribute.size;

        this.drawMode = drawMode;
        this.primitiveVerticesSize = stride * drawMode.indicesCount;

        vertices = BufferUtils.createByteBuffer(primitiveVerticesSize * 256 * 4);
        verticesPointerStart = memAddress0(vertices);

        indices = BufferUtils.createByteBuffer(drawMode.indicesCount * 512 * 4);
        indicesPointer = memAddress0(indices);

        vao = GL.genVertexArray();
        GL.bindVertexArray(vao);

        vbo = GL.genBuffer();
        GL.bindVertexBuffer(vbo);

        ibo = GL.genBuffer();
        GL.bindIndexBuffer(ibo);

        int offset = 0;
        for (int i = 0; i < attributes.length; i++) {
            Attrib attrib = attributes[i];

            GL.enableVertexAttribute(i);
            GL.vertexAttribute(i, attrib.count, attrib.getType(), attrib.normalized, stride, offset);

            offset += attrib.size;
        }

        GL.bindVertexArray(0);
        GL.bindVertexBuffer(0);
        GL.bindIndexBuffer(0);
    }

    public void destroy() {
        GL.deleteBuffer(ibo);
        GL.deleteBuffer(vbo);
        GL.deleteVertexArray(vao);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.begin() called while already building.");

        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;

        building = true;
        rendering3D = Utils.rendering3D;

        if (rendering3D) {
            Vec3d camera = mc.gameRenderer.getCamera().getPos();

            cameraX = camera.x;
            cameraZ = camera.z;
        }
        else {
            cameraX = 0;
            cameraZ = 0;
        }
    }

    public Mesh vec3(double x, double y, double z) {
        long p = verticesPointer;

        memPutFloat(p, (float) (x - cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float) (z - cameraZ));

        verticesPointer += 12;
        return this;
    }

    public Mesh vec2(double x, double y) {
        long p = verticesPointer;

        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);

        verticesPointer += 8;
        return this;
    }

    public Mesh color(Color c) {
        long p = verticesPointer;

        memPutByte(p, (byte) c.r);
        memPutByte(p + 1, (byte) c.g);
        memPutByte(p + 2, (byte) c.b);
        memPutByte(p + 3, (byte) (c.a * (float) alpha));

        verticesPointer += 4;
        return this;
    }

    public int next() {
        return vertexI++;
    }

    public void line(int i1, int i2) {
        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);

        indicesCount += 2;
        growIfNeeded();
    }

    public void quad(int i1, int i2, int i3, int i4) {
        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);

        indicesCount += 6;
        growIfNeeded();
    }

    public void triangle(int i1, int i2, int i3) {
        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        indicesCount += 3;
        growIfNeeded();
    }

    public void growIfNeeded() {
        // Vertices
        if ((vertexI + 1) * primitiveVerticesSize >= vertices.capacity()) {
            int offset = getVerticesOffset();

            int newSize = vertices.capacity() * 2;
            if (newSize % primitiveVerticesSize != 0) newSize += newSize % primitiveVerticesSize;

            ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(vertices), memAddress0(newVertices), offset);

            vertices = newVertices;
            verticesPointerStart = memAddress0(vertices);
            verticesPointer = verticesPointerStart + offset;
        }

        // Indices
        if (indicesCount * 4 >= indices.capacity()) {
            int newSize = indices.capacity() * 2;
            if (newSize % drawMode.indicesCount != 0) newSize += newSize % (drawMode.indicesCount * 4);

            ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);

            indices = newIndices;
            indicesPointer = memAddress0(indices);
        }
    }

    public void end() {
        if (!building) throw new IllegalStateException("Mesh.end() called while not building.");

        if (indicesCount > 0) {
            GL.bindVertexBuffer(vbo);
            GL.bufferData(GL_ARRAY_BUFFER, vertices.limit(getVerticesOffset()), GL_DYNAMIC_DRAW);
            GL.bindVertexBuffer(0);

            GL.bindIndexBuffer(ibo);
            GL.bufferData(GL_ELEMENT_ARRAY_BUFFER, indices.limit(indicesCount * 4), GL_DYNAMIC_DRAW);
            GL.bindIndexBuffer(0);
        }

        building = false;
    }

    public void beginRender(MatrixStack matrices) {
        GL.saveState();

        if (depthTest) GL.enableDepth();
        else GL.disableDepth();
        GL.enableBlend();
        GL.disableCull();
        GL.enableLineSmooth();

        if (rendering3D) {
            Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.pushMatrix();

            if (matrices != null) matrixStack.mul(matrices.peek().getPositionMatrix());

            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            matrixStack.translate(0, (float) -cameraPos.y, 0);
        }

        beganRendering = true;
    }

    public void render(MatrixStack matrices) {
        if (building) end();

        if (indicesCount > 0) {
            // Setup opengl state and matrix stack
            boolean wasBeganRendering = beganRendering;
            if (!wasBeganRendering) beginRender(matrices);

            // Render
            beforeRender();

            Shader.BOUND.setDefaults();

            GL.bindVertexArray(vao);
            GL.drawElements(drawMode.getGL(), indicesCount, GL_UNSIGNED_INT);

            // Cleanup opengl state and matrix stack
            GL.bindVertexArray(0);

            if (!wasBeganRendering) endRender();
        }
    }

    public void endRender() {
        if (rendering3D) RenderSystem.getModelViewStack().popMatrix();

        GL.restoreState();

        beganRendering = false;
    }

    public boolean isBuilding() {
        return building;
    }

    protected void beforeRender() {}

    private int getVerticesOffset() {
        return (int) (verticesPointer - verticesPointerStart);
    }
}
