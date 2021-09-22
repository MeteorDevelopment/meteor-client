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

    private void upload(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);

        upload(width, height, buffer, format, filterMin, filterMag);
    }

    private void upload(int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag) {
        this.width = width;
        this.height = height;

        if (!valid) {
            id = GL.genTexture();
            valid = true;
        }

        bind();
        GL.defaultPixelStore();

        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filterMin.toOpenGL());
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filterMag.toOpenGL());

        ((Buffer) buffer).rewind();
        GL.textureImage2D(GL_TEXTURE_2D, 0, format.toOpenGL(), width, height, 0, format.toOpenGL(), GL_UNSIGNED_BYTE, buffer);
    }

    public boolean isValid() {
        return valid;
    }

    public void bind() {
        GL.bindTexture(id);
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
        Linear;

        public int toOpenGL() {
            return this == Nearest ? GL_NEAREST : GL_LINEAR;
        }
    }
}
