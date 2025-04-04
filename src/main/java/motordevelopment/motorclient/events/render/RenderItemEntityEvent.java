/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.render;

import motordevelopment.motorclient.events.Cancellable;
import motordevelopment.motorclient.mixininterface.IEntityRenderState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;

public class RenderItemEntityEvent extends Cancellable {
    private static final RenderItemEntityEvent INSTANCE = new RenderItemEntityEvent();

    public ItemEntity itemEntity;
    public ItemEntityRenderState renderState;
    public float tickDelta;
    public MatrixStack matrixStack;
    public VertexConsumerProvider vertexConsumerProvider;
    public int light;
    public ItemModelManager itemModelManager;

    public static RenderItemEntityEvent get(ItemEntityRenderState renderState, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, ItemModelManager itemModelManager) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemEntity = (ItemEntity) ((IEntityRenderState) renderState).motor$getEntity();
        INSTANCE.renderState = renderState;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.vertexConsumerProvider = vertexConsumerProvider;
        INSTANCE.light = light;
        INSTANCE.itemModelManager = itemModelManager;
        return INSTANCE;
    }
}
