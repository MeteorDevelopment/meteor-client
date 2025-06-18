/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gl.DynamicUniformStorage;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class MeshUniforms {
    public static final int SIZE = new Std140SizeCalculator()
        .putMat4f()
        .putMat4f()
        .get();

    private static final Data DATA = new Data();

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Meteor - Mesh UBO", SIZE, 16);

    public static void flipFrame() {
        STORAGE.clear();
    }

    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        DATA.proj = proj;
        DATA.modelView = modelView;

        return STORAGE.write(DATA);
    }

    private static final class Data implements DynamicUniformStorage.Uploadable {
        private Matrix4f proj;
        private Matrix4f modelView;

        @Override
        public void write(ByteBuffer buffer) {
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
