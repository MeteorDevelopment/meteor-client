/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Cancellable {
    public Entity entity;
}
