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

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Meteor - Mesh UBO", SIZE, 16);

    public static void flipFrame() {
        STORAGE.clear();
    }

    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        return STORAGE.write(new Data(proj, modelView));
    }

    private record Data(Matrix4f proj, Matrix4f modelView) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putMat4f(proj)
                .putMat4f(modelView);
        }
    }
}
