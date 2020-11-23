/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;

public class WelcomeHud extends DoubleTextHudModule {
    public WelcomeHud(HUD hud) {
        super(hud, "welcome", "Displays a welcome message.", "Welcome to Meteor Client, ");

        rightColor = hud.welcomeColor();
    }

    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "UnknownPlayer!";

        return mc.player.getGameProfile().getName() + "!";
    }
}
