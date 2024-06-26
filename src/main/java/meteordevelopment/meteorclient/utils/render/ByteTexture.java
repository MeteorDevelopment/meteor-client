/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class ByteTexture extends AbstractTexture {
    public ByteTexture(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(width, height, data, format, filterMin, filterMag));
        } else {
            upload(width, height, data, format, filterMin, filterMag);
        }
    }

    public ByteTexture(int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(width, height, buffer, format, filterMin, filterMag));
        } else {
            upload(width, height, buffer, format, filterMin, filterMag);
        }
    }

    private void upload(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);

        upload(width, height, buffer, format, filterMin, filterMag);
    }

    private void upload(int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag) {
        bindTexture();

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

    @Override
    public void load(ResourceManager manager) throws IOException {}

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
