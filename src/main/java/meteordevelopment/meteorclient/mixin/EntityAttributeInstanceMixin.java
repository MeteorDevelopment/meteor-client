/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IEntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;

@Mixin(EntityAttributeInstance.class)
public abstract class EntityAttributeInstanceMixin implements IEntityAttributeInstance {
    @Shadow @Final private Map<UUID, EntityAttributeModifier> idToModifiers;
    @Shadow @Final private Map<UUID, EntityAttributeModifier> persistentModifiers; // Fixme: unsure
    @Shadow abstract Map<UUID, EntityAttributeModifier> getModifiers(EntityAttributeModifier.Operation operation);
    @Shadow protected abstract void onUpdate();

    @Override
    public void meteor$copyFrom(EntityAttributeInstance other) {
        for (UUID uuid : other.getModifiers().stream().map(EntityAttributeModifier::uuid).toList()) {
            @Nullable EntityAttributeModifier old = idToModifiers.put(uuid, other.getModifier(uuid));
            if (old != null) {
                getModifiers(old.operation()).remove(uuid);
                persistentModifiers.remove(uuid);
            }
            getModifiers(other.getModifier(uuid).operation()).put(uuid, other.getModifier(uuid));
        }
        onUpdate();
    }
}
