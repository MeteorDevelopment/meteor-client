/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class PlayerMoveEvent {
    private static final PlayerMoveEvent INSTANCE = new PlayerMoveEvent();

    public MoverType type;
    public Vec3 movement;

    public static PlayerMoveEvent get(MoverType type, Vec3 movement) {
        INSTANCE.type = type;
        INSTANCE.movement = movement;
        return INSTANCE;
    }
}
