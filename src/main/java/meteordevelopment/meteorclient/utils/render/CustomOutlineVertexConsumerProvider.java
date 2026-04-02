/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class CustomOutlineVertexConsumerProvider implements MultiBufferSource {
    private final MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        if (layer.isOutline()) {
            return new CustomVertexConsumer(this.immediate.getBuffer(layer));
        }

        var optional = layer.outline();
        if (optional.isPresent()) {
            return new CustomVertexConsumer(this.immediate.getBuffer(optional.get()));
        }

        return NoopVertexConsumer.INSTANCE;
    }

    public void draw() {
        immediate.endBatch();
    }

    private record CustomVertexConsumer(VertexConsumer consumer) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            consumer.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            consumer.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            consumer.setColor(argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            consumer.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
