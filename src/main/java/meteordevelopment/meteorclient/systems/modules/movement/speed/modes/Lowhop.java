/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.orbit.EventHandler;

public class Lowhop extends SpeedMode {
    public Lowhop() { super(SpeedModes.Lowhop); }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (!mc.player.isOnGround() || mc.player.isSneaking() || !mc.player.isSprinting()) return;
        ((IVec3d) event.movement).setY(getHop(0.40123128) * settings.hopHeight.get());
        mc.player.jump(); //mc.player.jump doesnt actually jump for some reason, it only gives horiz velocity. dont think about it too hard.
    }
}

