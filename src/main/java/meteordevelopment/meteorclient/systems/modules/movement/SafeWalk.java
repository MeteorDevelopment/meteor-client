/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class SafeWalk extends Module {
    public SafeWalk() {
        super(Categories.Movement, "safe-walk", "Prevents you from walking off blocks. Useful over a void.");
    }

    @EventHandler
    private void onClipAtLedge(ClipAtLedgeEvent event) {
        event.setClip(true);
    }
}
