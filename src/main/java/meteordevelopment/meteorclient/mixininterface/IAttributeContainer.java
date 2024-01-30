/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;

public interface IAttributeContainer {
    /**
     * Copy the {@link EntityAttributeInstance} of the other {@link AttributeContainer} into this one, copying their modifiers if there's a duplicate
     */
    void meteor$copyFrom(AttributeContainer other);
}
