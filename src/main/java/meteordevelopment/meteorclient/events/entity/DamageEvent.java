/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class DamageEvent {
    private static final DamageEvent INSTANCE = new DamageEvent();

    public LivingEntity entity;
    public DamageSource source;
    public float amount;

    public static DamageEvent get(LivingEntity entity, DamageSource source, float amount) {
        INSTANCE.entity = entity;
        INSTANCE.source = source;
        INSTANCE.amount = amount;
        return INSTANCE;
    }
}
