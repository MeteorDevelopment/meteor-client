/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.utils.entity.effects.EntityEffectCache;
import meteordevelopment.meteorclient.utils.entity.effects.StatusEffectBruteForce;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class StatusEffectHelper {
    @Nullable
    public static StatusEffectInstance getStatusEffect(LivingEntity entity, RegistryEntry<StatusEffect> effect) {
        if (entity == mc.player) return entity.getStatusEffect(effect);
        EntityEffectCache container = StatusEffectBruteForce.fetch(entity);
        return container == null ? null : container.statusEffects.get(effect);
    }

    public static boolean hasStatusEffect(LivingEntity entity, RegistryEntry<StatusEffect> effect) {
        if (entity == mc.player) return entity.hasStatusEffect(effect);
        EntityEffectCache container = StatusEffectBruteForce.fetch(entity);
        return container != null && container.statusEffects.containsKey(effect);
    }

    public static Collection<StatusEffectInstance> getStatusEffects(LivingEntity entity) {
        if (entity == mc.player) return entity.getStatusEffects();
        EntityEffectCache container = StatusEffectBruteForce.fetch(entity);
        return container == null ? List.of() : container.statusEffects.values();
    }

    public static Map<RegistryEntry<StatusEffect>, StatusEffectInstance> getActiveStatusEffects(LivingEntity entity) {
        if (entity == mc.player) return entity.getActiveStatusEffects();
        EntityEffectCache container = StatusEffectBruteForce.fetch(entity);
        return container == null ? Map.of() : container.statusEffects;
    }
}
