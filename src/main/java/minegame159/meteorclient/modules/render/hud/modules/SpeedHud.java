/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.Timer;
import minegame159.meteorclient.modules.render.hud.HUD;

public class SpeedHud extends DoubleTextHudModule {
    public SpeedHud(HUD hud) {
        super(hud, "speed", "Displays your horizontal speed.", "Speed: ");
    }

    @Override
    protected String getRight() {
        if (mc.player == null) return "0,0";

        double tX = Math.abs(mc.player.getX() - mc.player.prevX);
        double tZ = Math.abs(mc.player.getZ() - mc.player.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);

        if (Modules.get().isActive(Timer.class)){
            length *= Modules.get().get(Timer.class).getMultiplier();
        }

        return String.format("%.1f", length * 20);
    }
}
