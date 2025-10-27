/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.function.Supplier;

public class WrapperImmediateVertexConsumerProvider extends VertexConsumerProvider.Immediate {
    private final Supplier<VertexConsumerProvider> supplier;

    public WrapperImmediateVertexConsumerProvider(Supplier<VertexConsumerProvider> supplier) {
        super(null, null);
        this.supplier = supplier;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return supplier.get().getBuffer(layer);
    }

    @Override
    public void draw() {
    }

    @Override
    public void draw(RenderLayer layer) {
    }
}
