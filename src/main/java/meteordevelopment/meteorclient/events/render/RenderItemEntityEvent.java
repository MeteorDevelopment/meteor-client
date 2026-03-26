/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

public class RenderItemEntityEvent extends Cancellable {
    private static final RenderItemEntityEvent INSTANCE = new RenderItemEntityEvent();

    @Nullable
    public ItemEntity itemEntity;
    public ItemEntityRenderState renderState;
    public float tickDelta;
    public PoseStack matrixStack;
    public MultiBufferSource vertexConsumerProvider;
    public int light;
    public ItemModelResolver itemModelManager;
    public SubmitNodeCollector renderCommandQueue;

    public static RenderItemEntityEvent get(ItemEntityRenderState renderState, float tickDelta, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, ItemModelResolver itemModelManager, SubmitNodeCollector renderCommandQueue) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemEntity = (ItemEntity) ((IEntityRenderState) renderState).meteor$getEntity();
        INSTANCE.renderState = renderState;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.vertexConsumerProvider = vertexConsumerProvider;
        INSTANCE.light = light;
        INSTANCE.itemModelManager = itemModelManager;
        INSTANCE.renderCommandQueue = renderCommandQueue;
        return INSTANCE;
    }
}
