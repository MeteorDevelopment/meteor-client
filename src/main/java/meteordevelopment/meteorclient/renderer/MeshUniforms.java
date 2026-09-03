/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

public class MeshUniforms {
    public static final int SIZE = new Std140SizeCalculator()
        .putMat4f()
        .putMat4f()
        .get();

    private static final Data DATA = new Data();

    private static final DynamicGpuDataStorage<Data> STORAGE = new DynamicGpuDataStorageMapped<>("Meteor - Mesh UBO", SIZE, GpuBuffer.USAGE_UNIFORM, 16);

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        DATA.proj = proj;
        DATA.modelView = modelView;

        return STORAGE.writeData(DATA);
    }

    private static final class Data implements DynamicGpuDataStorage.DynamicGpuData {
        private Matrix4f proj;
        private Matrix4f modelView;

        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putMat4f(proj)
                .putMat4f(modelView);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
