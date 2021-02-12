/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import minegame159.meteorclient.waypoints.gui.EditWaypointScreen;
import minegame159.meteorclient.waypoints.gui.WWaypoint;
import net.minecraft.client.MinecraftClient;

public class TopBarWaypoints extends TopBarWindowScreen {
    public TopBarWaypoints() {
        super(TopBarType.Waypoints);

        refreshWidgetsOnInit = true;
    }

    @Override
    protected void initWidgets() {
        // Waypoints
        for (Waypoint waypoint : Waypoints.get()) {
            add(new WWaypoint(waypoint, () -> {
                clear();
                initWidgets();
            })).fillX().expandX();
            row();
        }

        // Add
        if (Utils.canUpdate()) {
            WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
            add.action = () -> MinecraftClient.getInstance().openScreen(new EditWaypointScreen(null));
        }
    }
}
