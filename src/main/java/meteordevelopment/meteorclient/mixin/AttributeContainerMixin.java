/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IAttributeContainer;
import meteordevelopment.meteorclient.mixininterface.IEntityAttributeInstance;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(AttributeContainer.class)
public abstract class AttributeContainerMixin implements IAttributeContainer {
    @Shadow @Final private Map<EntityAttribute, EntityAttributeInstance> custom;
    @Shadow @Final private Set<EntityAttributeInstance> tracked;

    @Override
    public void meteor$copyFrom(AttributeContainer other) {
        for (var otherInstance : ((AttributeContainerMixin) (Object) other).custom.values()) {
            @Nullable EntityAttributeInstance instance = custom.get(otherInstance.getAttribute());
            if (instance != null) {
                ((IEntityAttributeInstance) instance).meteor$copyFrom(otherInstance);
            } else {
                custom.put(otherInstance.getAttribute(), otherInstance);
                if (otherInstance.getAttribute().isTracked()) tracked.add(otherInstance);
            }
        }
    }
}
