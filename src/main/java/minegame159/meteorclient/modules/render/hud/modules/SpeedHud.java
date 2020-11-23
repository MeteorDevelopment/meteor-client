/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.Timer;
import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;

public class SpeedHud extends DoubleTextHudModule {
    public SpeedHud(HUD hud) {
        super(hud, "speed", "Displays your horizontal speed.", "Speed: ");
    }

    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "0,0";

        double tX = Math.abs(mc.player.getX() - mc.player.prevX);
        double tZ = Math.abs(mc.player.getZ() - mc.player.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);

        if (ModuleManager.INSTANCE.get(Timer.class).isActive()){
            length *= ModuleManager.INSTANCE.get(Timer.class).getMultiplier();
        }

        return String.format("%.1f", length * 20);
    }
}
