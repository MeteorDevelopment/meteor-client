/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import motordevelopment.motorclient.mixin.EntityRenderDispatcherMixin;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public interface IEntityRenderState {
    /**
     * Returns the entity that the render state refers to; necessary in scenarios when you want to perform an entity
     * rendering task with data that isn't present in the render state.<p>
     *
     * The entity is only set after the render state is retrieved in EntityRenderDispatcher#render, so make sure not
     * to call this before that point (e.g. mixing into an updateRenderState method), otherwise the entity returned will
     * not be the same one that the render state is referring to.
     *
     * @return The entity that the render state refers to
     *
     * @see EntityRenderDispatcherMixin#render$getAndUpdateRenderState(EntityRenderState, Entity, double, double, double, float, MatrixStack, VertexConsumerProvider, int, EntityRenderer)
     */
    Entity motor$getEntity();

    void motor$setEntity(Entity entity);
}
