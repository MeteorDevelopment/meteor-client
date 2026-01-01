/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

public class WaypointAddEvent {
    private static final WaypointAddEvent INSTANCE = new WaypointAddEvent();

    public Waypoint waypoint;

    public static WaypointAddEvent get(Waypoint waypoint) {
        INSTANCE.waypoint = waypoint;
        return INSTANCE;
    }
}