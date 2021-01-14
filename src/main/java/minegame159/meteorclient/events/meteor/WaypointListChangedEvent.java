/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.meteor;

public class WaypointListChangedEvent {
    private static final WaypointListChangedEvent INSTANCE = new WaypointListChangedEvent();

    public static WaypointListChangedEvent get() {
        return INSTANCE;
    }
}
