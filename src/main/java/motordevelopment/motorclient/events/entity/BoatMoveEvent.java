/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity;

import net.minecraft.entity.vehicle.AbstractBoatEntity;

public class BoatMoveEvent {
    private static final BoatMoveEvent INSTANCE = new BoatMoveEvent();

    public AbstractBoatEntity boat;

    public static BoatMoveEvent get(AbstractBoatEntity entity) {
        INSTANCE.boat = entity;
        return INSTANCE;
    }
}
