/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.ClipAtLedgeEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class SafeWalk extends Module {
    public SafeWalk() {
        super(Category.Movement, "Safe-walk", "Prevents you from walking off blocks. Useful over a void.");
    }

    @EventHandler
    private final Listener<ClipAtLedgeEvent> onClipAtLedge = new Listener<>(event -> {
        event.setClip(true);
    });
}
