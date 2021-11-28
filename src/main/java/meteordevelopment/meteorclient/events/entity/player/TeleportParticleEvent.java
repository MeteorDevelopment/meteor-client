/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

public class TeleportParticleEvent {
    private static final TeleportParticleEvent INSTANCE = new TeleportParticleEvent();

    public double x, y, z;

    public static TeleportParticleEvent get(double x, double y, double z) {
        INSTANCE.x = x;
        INSTANCE.y = y;
        INSTANCE.z = z;
        return INSTANCE;
    }
}
