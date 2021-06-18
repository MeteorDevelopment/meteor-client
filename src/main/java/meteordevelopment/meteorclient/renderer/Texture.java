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
import static org.lwjgl.opengl.GL12C.GL_UNPACK_IMAGE_HEIGHT;
import static org.lwjgl.opengl.GL12C.GL_UNPACK_SKIP_IMAGES;

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
            id = glGenTextures();
            valid = true;
        }

        bind();

        glPixelStorei(GL_UNPACK_SWAP_BYTES, GL_FALSE);
        glPixelStorei(GL_UNPACK_LSB_FIRST, GL_FALSE);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filterMin.toOpenGL());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filterMag.toOpenGL());

        ((Buffer) buffer).rewind();
        glTexImage2D(GL_TEXTURE_2D, 0, format.toOpenGL(), width, height, 0, format.toOpenGL(), GL_UNSIGNED_BYTE, buffer);
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
