/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.effects;

import net.minecraft.entity.effect.StatusEffect;

import java.util.Objects;

public class MutableParticleColor {
    public static final MutableParticleColor EMPTY = new MutableParticleColor();

    public float r, g, b;
    public int a;

    public void add(StatusEffect effect, int amplifier) {
        int color = effect.getColor();
        r += (float)(amplifier * (color >> 16 & 255)) / 255.0F;
        g += (float)(amplifier * (color >> 8 & 255)) / 255.0F;
        b += (float)(amplifier * (color & 255)) / 255.0F;
        a += amplifier;
    }

    public void add(StatusEffect effect) {
        add(effect, 1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof MutableParticleColor other) {
            return r == other.r && g == other.g && b == other.b && a == other.a;
        }
        return false;
    }
}
