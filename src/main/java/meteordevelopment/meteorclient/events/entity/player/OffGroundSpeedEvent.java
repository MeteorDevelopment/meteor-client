/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

public class OffGroundSpeedEvent {
    public static final OffGroundSpeedEvent INSTANCE = new OffGroundSpeedEvent();

    public float speed;

    public static OffGroundSpeedEvent get(float speed) {
        INSTANCE.speed = speed;
        return INSTANCE;
    }
}
