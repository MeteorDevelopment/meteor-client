/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.entity.attribute.EntityAttributeInstance;

public interface IEntityAttributeInstance {
    /**
     * Adds the modifiers of the other {@link EntityAttributeInstance} to this one, replacing them if there's a duplicate
     */
    void meteor$copyFrom(EntityAttributeInstance other);
}
