/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

public class CustomOutlineVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536));

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        if (layer.isOutline()) {
            return new CustomVertexConsumer(this.immediate.getBuffer(layer));
        }

        var optional = layer.getAffectedOutline();
        if (optional.isPresent()) {
            return new CustomVertexConsumer(this.immediate.getBuffer(optional.get()));
        }

        return NoopVertexConsumer.INSTANCE;
    }

    public void draw() {
        immediate.draw();
    }

    private record CustomVertexConsumer(VertexConsumer consumer) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            consumer.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            consumer.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            consumer.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }
}
