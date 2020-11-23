/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.waypoints.gui;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.waypoints.Waypoint;

public class WWaypointIcon extends WWidget {
    private final Waypoint waypoint;

    public WWaypointIcon(Waypoint waypoint) {
        this.waypoint = waypoint;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 32;
        height = 32;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.post(() -> waypoint.renderIcon(x, y, 0, 1, 32));
    }
}
