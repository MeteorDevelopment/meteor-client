/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering.gl;

import static org.lwjgl.opengl.GL30C.*;

public class Mesh {
    private final int vao, vbo, ibo;
    private final int indicesCount;

    public Mesh(float[] vertices, int[] indices, int... attributes) {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 0;
        for (int a : attributes) stride += a * 4;

        int offset = 0;
        for (int i = 0; i < attributes.length; i++) {
            glEnableVertexAttribArray(i);
            glVertexAttribPointer(i, attributes[i], GL_FLOAT, false, stride, offset);
            offset += attributes[i] * 4;
        }

        ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        indicesCount = indices.length;

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void bind() {
        glBindVertexArray(vao);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
    }

    public void renderMesh() {
        glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0);
    }

    public void unbind() {
        glBindVertexArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void render() {
        bind();
        renderMesh();
        unbind();
    }
}
