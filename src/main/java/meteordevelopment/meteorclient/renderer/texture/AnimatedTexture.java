/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.mixininterface.IGlCommandEncoder;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class AnimatedTexture extends AbstractTexture implements ITextureGetter {

    private final int[] delays;

    public AnimatedTexture(int width, int height, int frames, int[] delays, TextureFormat format, FilterMode min, FilterMode mag) {
        glTexture = ((IGpuDevice)RenderSystem.getDevice()).meteor$createAnimatedTexture("", 15, format, width, height, frames, 1);
        sampler = RenderSystem.getSamplerCache().get(AddressMode.REPEAT, AddressMode.REPEAT, min, mag, false);
        glTextureView = RenderSystem.getDevice().createTextureView(glTexture);
        this.delays = delays;
    }

    public int getWidth() {
        return getGlTexture().getWidth(0);
    }

    public int getHeight() {
        return getGlTexture().getHeight(0);
    }

    public int getFrames() { return getGlTexture().getDepthOrLayers(); }

    public int[] getDelays() {
        return delays;
    }

    public void upload(byte[] bytes) {
        upload(BufferUtils.createByteBuffer(bytes.length).put(bytes));
    }

    public void upload(ByteBuffer buffer) {
        var image = getImage();

        buffer.rewind();
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), image.imageId(), buffer.remaining());

        ((IGlCommandEncoder)RenderSystem.getDevice().createCommandEncoder()).meteor_client$writeToAnimTexture(glTexture, image);

        image.close();
    }

    private static NativeImage.Format getFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8 -> NativeImage.Format.RGBA;
            case RED8 -> NativeImage.Format.LUMINANCE;
            default -> throw new IllegalArgumentException();
        };
    }

    private @NotNull AnimatedNativeImage getImage() {
        NativeImage.Format imageFormat = getFormat(glTexture.getFormat());
        return new AnimatedNativeImage(imageFormat, getWidth(), getHeight(), getFrames(), true);
    }

    public static AnimatedTexture readResource(String path, boolean flipY, FilterMode filter) throws IOException {
        try (var in = Texture.class.getResourceAsStream(path)) {
            if (in == null) return null;

            ByteBuffer gifData = TextureUtil.readResource(in).rewind();

            return readBuffer(gifData, flipY, filter);
        }
    }

    public static AnimatedTexture readBuffer(ByteBuffer buffer, boolean flipY, FilterMode filter) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer framesBfr = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            PointerBuffer delays = stack.mallocPointer(1);

            if (!STBImage.stbi_info_from_memory(buffer, width, height, comp)) throw new IOException("Failed to read image data with STBI");

            STBImage.stbi_set_flip_vertically_on_load(flipY);
            ByteBuffer image = STBImage.stbi_load_gif_from_memory(buffer, delays, width, height, framesBfr,
                comp, STBImage.STBI_rgb_alpha);

            int frames = framesBfr.get(0);

            IntBuffer delaysBuffer = MemoryUtil.memIntBuffer(delays.get(0), frames);

            int[] delaysArray = new int[frames];

            for (int i = 0; i < frames; i++) {
                delaysArray[i] = delaysBuffer.get(i);
            }

            AnimatedTexture texture = new AnimatedTexture(width.get(0), height.get(0), frames,
                delaysArray, TextureFormat.RGBA8, filter, filter);

            texture.upload(image);

            STBImage.stbi_image_free(image);
            STBImage.stbi_set_flip_vertically_on_load(false);

            return texture;
        }
    }
}
