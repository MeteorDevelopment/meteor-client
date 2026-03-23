/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;

/**
 * UBO storage with a constant size. Exceeding this size causes an {@link IndexOutOfBoundsException} to be thrown.
 *
 * @see DynamicUniformStorage
 * @author Crosby
 */
public class FixedUniformStorage<T extends DynamicUniformStorage.Uploadable> {
    private final MappableRingBuffer buffer;
    private final int blockSize;
    private final int capacity;
    private int size;

    public FixedUniformStorage(String name, int blockSize, int capacity) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.blockSize = MathHelper.roundUpToMultiple(blockSize, gpuDevice.getUniformOffsetAlignment());
        this.capacity = capacity;
        int alignedCapacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
        this.size = 0;
        this.buffer = new MappableRingBuffer(() -> name + " x" + this.blockSize, 130, this.blockSize * alignedCapacity);
    }

    public GpuBufferSlice write(T value) {
        if (this.size >= this.capacity) {
            throw new IndexOutOfBoundsException(String.format("Index %s out of bounds for length %s", this.size, this.capacity));
        } else {
            int i = this.size * this.blockSize;
            GpuBufferSlice slice = this.buffer.getBlocking().slice(i, this.blockSize);

            try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
                .createCommandEncoder()
                .mapBuffer(slice, false, true)) {
                value.write(mappedView.data());
            }

            this.size++;
            return slice;
        }
    }

    public GpuBufferSlice[] writeAll(T[] values) {
        if (values.length == 0) {
            return new GpuBufferSlice[0];
        } else if (this.size + values.length > this.capacity) {
            throw new IndexOutOfBoundsException(String.format("Index %s out of bounds for length %s", this.size + values.length - 1, this.capacity));
        } else {
            int i = this.size * this.blockSize;
            GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[values.length];
            GpuBuffer ubo = this.buffer.getBlocking();

            try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
                .createCommandEncoder()
                .mapBuffer(ubo.slice(i, values.length * this.blockSize), false, true)) {
                ByteBuffer byteBuffer = mappedView.data();

                for (int j = 0; j < values.length; j++) {
                    T uploadable = values[j];
                    gpuBufferSlices[j] = ubo.slice(i + j * this.blockSize, this.blockSize);
                    byteBuffer.position(j * this.blockSize);
                    uploadable.write(byteBuffer);
                }
            }

            this.size += values.length;
            return gpuBufferSlices;
        }
    }

    public void clear() {
        this.size = 0;
        this.buffer.rotate();
    }

    public void close() {
        this.buffer.close();
    }
}
