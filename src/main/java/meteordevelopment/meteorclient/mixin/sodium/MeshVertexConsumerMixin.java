/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.utils.render.MeshVertexConsumerProvider;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MeshVertexConsumerProvider.MeshVertexConsumer.class, remap = false)
public abstract class MeshVertexConsumerMixin implements VertexConsumer, VertexBufferWriter {
    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        int positionOffset = format.getElementOffset(CommonVertexAttribute.POSITION);

        if (positionOffset == -1) return;

        for (int i = 0; i < count; i++) {
            long positionPtr = ptr + (long) format.stride() * i + positionOffset;

            float x = MemoryUtil.memGetFloat(positionPtr);
            float y = MemoryUtil.memGetFloat(positionPtr + 4);
            float z = MemoryUtil.memGetFloat(positionPtr + 8);

            vertex(x, y, z);
            next();
        }
    }
}
