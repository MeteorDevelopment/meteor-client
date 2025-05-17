/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Texture extends AbstractTexture {
    public Texture(int width, int height, TextureFormat format, FilterMode min, FilterMode mag) {
        /*
         * todo wtf should the usage be? (i)
         *
         *  I assume the magic numbers across the codebase from 1 - 15 are inlined combinations of these constants from com.mojang.blaze3d.textures.GpuTexture
         *  public static final int USAGE_COPY_DST = 1;
         *	public static final int USAGE_COPY_SRC = 2;
         *	public static final int USAGE_TEXTURE_BINDING = 4;
         *	public static final int USAGE_RENDER_ATTACHMENT = 8;
         *
         *  Is only used as of writing in net.minecraft.client.gl.GlCommandEncoder, where it performs a lot of checks on
         *  the texture usage and if it isn't marked appropriately it throws an exception. Just leaving it as 15 for now
         *  because that should pass all the checks.
         */
        glTexture = RenderSystem.getDevice().createTexture("", 15, format, width, height, 1, 1);
        glTexture.setTextureFilter(min, mag, false);
    }

    public int getWidth() {
        return getGlTexture().getWidth(0);
    }

    public int getHeight() {
        return getGlTexture().getHeight(0);
    }

    public void upload(byte[] bytes) {
        upload(BufferUtils.createByteBuffer(bytes.length).put(bytes));
    }

    public void upload(ByteBuffer buffer) {
        var image = getImage();

        buffer.rewind();
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), image.imageId(), buffer.remaining());

        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
            glTexture,
            image,
            0,
            0,
            0,
            0,
            getWidth(),
            getHeight(),
            0,
            0
        );

        image.close();
    }

    private @NotNull NativeImage getImage() {
        NativeImage.Format imageFormat = switch (glTexture.getFormat()) {
            case RGBA8 -> NativeImage.Format.RGBA;
            case RED8 -> NativeImage.Format.LUMINANCE;
            default -> throw new IllegalArgumentException();
        };

        // Workaround for writeToTexture(IntBuffer) overload comparing width * height to the size of the int buffer.
        // And since we are working with pixels which are only one byte in size, the sizes don't match
        return new NativeImage(imageFormat, getWidth(), getHeight(), false);
    }
}
