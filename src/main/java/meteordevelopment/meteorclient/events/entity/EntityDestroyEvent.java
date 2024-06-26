/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import net.minecraft.entity.Entity;

public class EntityDestroyEvent {
    private static final EntityDestroyEvent INSTANCE = new EntityDestroyEvent();

    public Entity entity;

    public static EntityDestroyEvent get(Entity entity) {
        INSTANCE.entity = entity;
        return INSTANCE;
    }
}
