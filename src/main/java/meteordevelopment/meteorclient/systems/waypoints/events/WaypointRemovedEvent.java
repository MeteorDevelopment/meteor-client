/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

public class WaypointRemovedEvent {

    public final Waypoint waypoint;

    public WaypointRemovedEvent(Waypoint waypoint) {
        this.waypoint = waypoint;
    }
}
