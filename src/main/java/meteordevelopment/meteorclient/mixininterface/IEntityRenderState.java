/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.entity.Entity;

public interface IEntityRenderState {
    Entity meteor$getEntity();

    void meteor$setEntity(Entity entity);
}
