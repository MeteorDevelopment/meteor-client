/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.NameProtect;
import minegame159.meteorclient.modules.render.hud.HUD;

public class WelcomeHud extends DoubleTextHudModule {
    public WelcomeHud(HUD hud) {
        super(hud, "welcome", "Displays a welcome message.", "Welcome to Meteor Client, ");
        rightColor = hud.welcomeColor();
    }

    @Override
    protected String getRight() {
        if (mc.player == null) return "UnknownPlayer!";
        return ModuleManager.INSTANCE.get(NameProtect.class).getName(mc.player.getGameProfile().getName()) + "!";
    }
}
