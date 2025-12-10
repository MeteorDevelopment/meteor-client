/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import meteordevelopment.meteorclient.mixin.EntityRenderManagerMixin;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

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
     * @see EntityRenderManagerMixin#getAndUpdateRenderState$setEntity(EntityRenderState, Entity, float)
     */
    @Nullable(value = "EntityCulling mod can prevent the code that sets the entity from running")
    Entity meteor$getEntity();

    void meteor$setEntity(Entity entity);
}
