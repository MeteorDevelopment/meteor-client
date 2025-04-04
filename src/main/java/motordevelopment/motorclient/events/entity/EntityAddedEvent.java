/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity;

import net.minecraft.entity.Entity;

public class EntityAddedEvent {
    private static final EntityAddedEvent INSTANCE = new EntityAddedEvent();

    public Entity entity;

    public static EntityAddedEvent get(Entity entity) {
        INSTANCE.entity = entity;
        return INSTANCE;
    }
}
