/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

public class NoopOutlineVertexConsumerProvider extends OutlineVertexConsumerProvider {
    public static final NoopOutlineVertexConsumerProvider INSTANCE = new NoopOutlineVertexConsumerProvider();

    private NoopOutlineVertexConsumerProvider() {
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return NoopVertexConsumer.INSTANCE;
    }

    @Override
    public void draw() {
    }
}
