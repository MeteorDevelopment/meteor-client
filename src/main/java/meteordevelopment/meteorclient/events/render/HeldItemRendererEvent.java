/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class HeldItemRendererEvent {
    private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();

    public AbstractClientPlayerEntity player;
    public float tickDelta, pitch, swingProgress, equipProgress;
    public Hand hand;
    public ItemStack item;
    public MatrixStack matrix;
    public VertexConsumerProvider vertex;
    public int light;

    public static HeldItemRendererEvent get(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        INSTANCE.equipProgress = equipProgress;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.pitch = pitch;
        INSTANCE.player = player;
        INSTANCE.hand = hand;
        INSTANCE.swingProgress = swingProgress;
        INSTANCE.matrix = matrices;
        INSTANCE.vertex = vertexConsumers;
        INSTANCE.light = light;
        return INSTANCE;
    }
}
