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
        glTexture = RenderSystem.getDevice().createTexture("", 15, format, width, height, 1, 1);
        glTexture.setTextureFilter(min, mag, false);

        glTextureView = RenderSystem.getDevice().createTextureView(glTexture);
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

        RenderSystem.getDevice().createCommandEncoder().writeToTexture(glTexture, image);

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
