/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.effects;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Map;

public class EntityEffectCache {
    public final Map<StatusEffect, StatusEffectInstance> statusEffects = new Reference2ReferenceOpenHashMap<>();
    public int particleColor;

    public void add(StatusEffect effect, int amplifier) {
        statusEffects.put(effect, new StatusEffectInstance(effect, amplifier - 1));
    }

    public void add(StatusEffect effect) {
        add(effect, 1);
    }
}
