/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class DamageEvent {
    public LivingEntity entity;
    public DamageSource source;
}
