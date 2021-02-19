/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.ClipAtLedgeEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;

public class SafeWalk extends Module {
    public SafeWalk() {
        super(Categories.Movement, "safe-walk", "Prevents you from walking off blocks. Useful over a void.");
    }

    @EventHandler
    private void onClipAtLedge(ClipAtLedgeEvent event) {
        event.setClip(true);
    }
}
