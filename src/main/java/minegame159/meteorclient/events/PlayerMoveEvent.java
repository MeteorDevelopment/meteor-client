/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class PlayerMoveEvent {
    public MovementType type;
    public Vec3d movement;
}
