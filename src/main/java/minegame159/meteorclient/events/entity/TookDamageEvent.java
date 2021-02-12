/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class TookDamageEvent {
    private static final TookDamageEvent INSTANCE = new TookDamageEvent();

    public LivingEntity entity;
    public DamageSource source;

    public static TookDamageEvent get(LivingEntity entity, DamageSource source) {
        INSTANCE.entity = entity;
        INSTANCE.source = source;
        return INSTANCE;
    }
}
