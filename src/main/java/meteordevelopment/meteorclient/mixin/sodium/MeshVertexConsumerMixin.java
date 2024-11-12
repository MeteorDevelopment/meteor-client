/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.utils.render.MeshVertexConsumerProvider;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MeshVertexConsumerProvider.MeshVertexConsumer.class, remap = false)
public abstract class MeshVertexConsumerMixin implements VertexConsumer, VertexBufferWriter {
    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);

        if (positionOffset == -1) return;

        for (int i = 0; i < count; i++) {
            long positionPtr = ptr + (long) format.getVertexSizeByte() * i + positionOffset;

            float x = MemoryUtil.memGetFloat(positionPtr);
            float y = MemoryUtil.memGetFloat(positionPtr + 4);
            float z = MemoryUtil.memGetFloat(positionPtr + 8);

            vertex(x, y, z);
        }
    }
}
