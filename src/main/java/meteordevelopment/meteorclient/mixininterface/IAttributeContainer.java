/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;

public interface IAttributeContainer {
    /**
     * Adds the {@link EntityAttributeInstance} of the other {@link AttributeContainer} to this one, making a union of their modifiers if there's a duplicate
     */
    void meteor$union(AttributeContainer other);
}
