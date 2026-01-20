/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.LOG;

public class AnimatedNativeImage implements AutoCloseable {
    private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("NativeImage3D");
    private final NativeImage.Format format;
    private final int width;
    private final int height;
    private final int layers;
    private final boolean isStbImage;
    private long pointer;
    private final long sizeBytes;

    private static final Set<StandardOpenOption> WRITE_TO_FILE_OPEN_OPTIONS = EnumSet.of(
        StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
    );

    public AnimatedNativeImage(int width, int height, int layers, boolean useStb) {
        this(NativeImage.Format.RGBA, width, height, layers, useStb);
    }

    public AnimatedNativeImage(NativeImage.Format format, int width, int height, int layers, boolean useStb) {
        // TODO mixin this into NativeImage constructor with a new layers field?
        if (width > 0 && height > 0 && layers > 0) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.layers = layers;
            this.sizeBytes = (long)width * height * layers * format.getChannelCount();
            this.isStbImage = false;
            if (useStb) {
                this.pointer = MemoryUtil.nmemCalloc(1L, this.sizeBytes);
            } else {
                this.pointer = MemoryUtil.nmemAlloc(this.sizeBytes);
            }

            MEMORY_POOL.malloc(this.pointer, (int)this.sizeBytes);
            if (this.pointer == 0L) {
                throw new IllegalStateException("Unable to allocate texture of size " + width + "x" + height + "x" + layers + " (" + format.getChannelCount() + " channels)");
            }
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height + "x" + layers);
        }
    }

    public AnimatedNativeImage(NativeImage.Format format, int width, int height, int layers, boolean useStb, long pointer) {
        if (width > 0 && height > 0) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.layers = layers;
            this.isStbImage = useStb;
            this.pointer = pointer;
            this.sizeBytes = (long)width * height * layers * format.getChannelCount();
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height + "x" + layers);
        }
    }
    @Override
    public void close() {
        if (this.pointer != 0L) {
            if (this.isStbImage) {
                STBImage.nstbi_image_free(this.pointer);
            } else {
                MemoryUtil.nmemFree(this.pointer);
            }

            MEMORY_POOL.free(this.pointer);
        }

        this.pointer = 0L;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLayers() {
        return this.layers;
    }

    public NativeImage.Format getFormat() {
        return this.format;
    }

    public long imageId() {
        return this.pointer;
    }
}
