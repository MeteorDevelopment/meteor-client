/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.meteor;

public class MiddleMouseButtonEvent {
    private static final MiddleMouseButtonEvent INSTANCE = new MiddleMouseButtonEvent();

    public static MiddleMouseButtonEvent get() {
        return INSTANCE;
    }
}
