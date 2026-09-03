/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

public class OutlineUniforms {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putInt()
        .putFloat()
        .putInt()
        .putFloat()
        .get();

    private static final DynamicGpuDataStorage<Data> STORAGE = new DynamicGpuDataStorageMapped<>("Meteor - Outline UBO", UNIFORM_SIZE, GpuBuffer.USAGE_UNIFORM, 16);

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static GpuBufferSlice write(int width, float fillOpacity, int shapeMode, float glowMultiplier) {
        return STORAGE.writeData(new Data(width, fillOpacity, shapeMode, glowMultiplier));
    }

    private record Data(int width, float fillOpacity, int shapeMode,
                        float glowMultiplier) implements DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putInt(width)
                .putFloat(fillOpacity)
                .putInt(shapeMode)
                .putFloat(glowMultiplier);
        }
    }
}
