/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;

public class Texture {
    public int width, height;
    private int id;
    private boolean valid;

    public Texture(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
        if (RenderSystem.isOnRenderThread()) {
            upload(width, height, data, format, filterMin, filterMag);
        }
        else {
            RenderSystem.recordRenderCall(() -> upload(width, height, data, format, filterMin, filterMag));
        }
    }

    public Texture() {}

    private void upload(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);

        upload(width, height, buffer, format, filterMin, filterMag, false);
    }

    public void upload(int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag, boolean wrapClamp) {
        this.width = width;
        this.height = height;

        if (!valid) {
            id = GL.genTexture();
            valid = true;
        }

        bind();
        GL.defaultPixelStore();

        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapClamp ? GL_CLAMP_TO_EDGE : GL_REPEAT);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapClamp ? GL_CLAMP_TO_EDGE : GL_REPEAT);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filterMin.toOpenGL());
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filterMag.toOpenGL());

        ((Buffer) buffer).rewind();
        GL.textureImage2D(GL_TEXTURE_2D, 0, format.toOpenGL(), width, height, 0, format.toOpenGL(), GL_UNSIGNED_BYTE, buffer);

        if (filterMin == Filter.LinearMipmapLinear || filterMag == Filter.LinearMipmapLinear) {
            GL.generateMipmap(GL_TEXTURE_2D);
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void bind(int slot) {
        GL.bindTexture(id, slot);
    }
    public void bind() {
        bind(0);
    }

    public void dispose() {
        GL.deleteTexture(id);
        valid = false;
    }

    public enum Format {
        A,
        RGB,
        RGBA;

        public int toOpenGL() {
            return switch (this) {
                case A -> GL_RED;
                case RGB -> GL_RGB;
                case RGBA -> GL_RGBA;
            };
        }
    }

    public enum Filter {
        Nearest,
        Linear,
        LinearMipmapLinear;

        public int toOpenGL() {
            return switch (this) {
                case Nearest -> GL_NEAREST;
                case Linear -> GL_LINEAR;
                case LinearMipmapLinear -> GL_LINEAR_MIPMAP_LINEAR;
            };
        }
    }
}
