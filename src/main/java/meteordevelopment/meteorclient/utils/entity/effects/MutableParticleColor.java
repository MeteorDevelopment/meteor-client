/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.effects;

import net.minecraft.entity.effect.StatusEffect;

public class MutableParticleColor {
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
}
