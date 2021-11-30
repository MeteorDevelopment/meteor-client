/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    public enum Attrib {
        Float(1),
        Vec2(2),
        Vec3(3),
        Color(4);

        public final int size;

        Attrib(int size) {
            this.size = size;
        }
    }

    public boolean depthTest = false;
    public double alpha = 1;

    private final DrawMode drawMode;
    private final int primitiveVerticesSize;

    private final int vao, vbo, ibo;

    private ByteBuffer vertices;
    private long verticesPointer;
    private int vertexComponentCount;

    private ByteBuffer indices;
    private long indicesPointer;

    private int vertexI, indicesCount;

    private boolean building, rendering3D;
    private double cameraX, cameraZ;
    private boolean beganRendering;

    public Mesh(DrawMode drawMode, Attrib... attributes) {
        int stride = 0;
        for (Attrib attribute : attributes) stride += attribute.size * 4;

        this.drawMode = drawMode;
        this.primitiveVerticesSize = stride * drawMode.indicesCount;

        vertices = BufferUtils.createByteBuffer(primitiveVerticesSize * 256 * 4);
        verticesPointer = memAddress0(vertices);

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
            int attribute = attributes[i].size;

            GL.enableVertexAttribute(i);
            GL.vertexAttribute(i, attribute, GL_FLOAT, false, stride, offset);

            offset += attribute * 4;
        }

        GL.bindVertexArray(0);
        GL.bindVertexBuffer(0);
        GL.bindIndexBuffer(0);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.end() called while already building.");

        vertexComponentCount = 0;
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
        long p = verticesPointer + vertexComponentCount * 4L;

        memPutFloat(p, (float) (x - cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float) (z - cameraZ));

        vertexComponentCount += 3;
        return this;
    }

    public Mesh vec2(double x, double y) {
        long p = verticesPointer + vertexComponentCount * 4L;

        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);

        vertexComponentCount += 2;
        return this;
    }

    public Mesh color(Color c) {
        long p = verticesPointer + vertexComponentCount * 4L;

        memPutFloat(p, c.r / 255f);
        memPutFloat(p + 4, c.g / 255f);
        memPutFloat(p + 8, c.b / 255f);
        memPutFloat(p + 12, c.a / 255f * (float) alpha);

        vertexComponentCount += 4;
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

    public void growIfNeeded() {
        // Vertices
        if ((vertexI + 1) * primitiveVerticesSize >= vertices.capacity()) {
            int newSize = vertices.capacity() * 2;
            if (newSize % primitiveVerticesSize != 0) newSize += newSize % primitiveVerticesSize;

            ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(vertices), memAddress0(newVertices), vertexComponentCount * 4L);

            vertices = newVertices;
            verticesPointer = memAddress0(vertices);
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
            GL.bufferData(GL_ARRAY_BUFFER, vertices.limit(vertexComponentCount * 4), GL_DYNAMIC_DRAW);
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
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();

            if (matrices != null) matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());

            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            matrixStack.translate(0, -cameraPos.y, 0);
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
        if (rendering3D) RenderSystem.getModelViewStack().pop();

        GL.restoreState();

        beganRendering = false;
    }

    protected void beforeRender() {}
}
