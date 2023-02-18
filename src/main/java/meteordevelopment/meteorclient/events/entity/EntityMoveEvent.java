/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class EntityMoveEvent {
    private static final EntityMoveEvent INSTANCE = new EntityMoveEvent();

    public Entity entity;
    public Vec3d movement;

    public static EntityMoveEvent get(Entity entity, Vec3d movement) {
        INSTANCE.entity = entity;
        INSTANCE.movement = movement;
        return INSTANCE;
    }
}
