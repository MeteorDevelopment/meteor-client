/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.system.MemoryUtil.*;

public class MeshBuilder {
    public double alpha = 1;

    private final VertexFormat format;
    private final int primitiveVerticesSize;
    private final int primitiveIndicesCount;

    private ByteBuffer vertices;
    private long verticesPointerStart, verticesPointer;

    private ByteBuffer indices;
    private long indicesPointer;

    private int vertexI, indicesCount;

    private boolean building;
    private double cameraX, cameraZ;

    public MeshBuilder(RenderPipeline pipeline) {
        this(pipeline.getVertexFormat(), pipeline.getVertexFormatMode());
    }

    public MeshBuilder(VertexFormat format, VertexFormat.DrawMode drawMode) {
        this.format = format;
        primitiveVerticesSize = format.getVertexSize() * drawMode.firstVertexCount;
        primitiveIndicesCount = drawMode.firstVertexCount;

        vertices = BufferUtils.createByteBuffer(primitiveVerticesSize * 256 * 4);
        verticesPointerStart = memAddress0(vertices);

        indices = BufferUtils.createByteBuffer(primitiveIndicesCount * 512 * 4);
        indicesPointer = memAddress0(indices);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.begin() called while already building.");

        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;

        building = true;

        if (Utils.rendering3D) {
            Vec3d camera = mc.gameRenderer.getCamera().getPos();

            cameraX = camera.x;
            cameraZ = camera.z;
        }
        else {
            cameraX = 0;
            cameraZ = 0;
        }
    }

    public MeshBuilder vec3(double x, double y, double z) {
        long p = verticesPointer;

        memPutFloat(p, (float) (x - cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float) (z - cameraZ));

        verticesPointer += 12;
        return this;
    }

    public MeshBuilder vec2(double x, double y) {
        long p = verticesPointer;

        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);

        verticesPointer += 8;
        return this;
    }

    public MeshBuilder color(Color c) {
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
            if (newSize % primitiveIndicesCount != 0) newSize += newSize % (primitiveIndicesCount * 4);

            ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);

            indices = newIndices;
            indicesPointer = memAddress0(indices);
        }
    }

    public void end() {
        if (!building) throw new IllegalStateException("Mesh.end() called while not building.");

        building = false;
    }

    public boolean isBuilding() {
        return building;
    }

    public GpuBuffer getVertexBuffer() {
        vertices.limit(getVerticesOffset());
        return format.uploadImmediateVertexBuffer(vertices);
    }

    public GpuBuffer getIndexBuffer() {
        indices.limit(indicesCount * 4);
        return format.uploadImmediateIndexBuffer(indices);
    }

    public int getIndicesCount() {
        return indicesCount;
    }

    private int getVerticesOffset() {
        return (int) (verticesPointer - verticesPointerStart);
    }
}
