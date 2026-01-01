/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

public class WaypointRemoveEvent {
    private static final WaypointRemoveEvent INSTANCE = new WaypointRemoveEvent();

    public Waypoint waypoint;

    public static WaypointRemoveEvent get(Waypoint waypoint) {
        INSTANCE.waypoint = waypoint;
        return INSTANCE;
    }
}