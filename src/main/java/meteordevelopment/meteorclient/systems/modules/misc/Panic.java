/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.hud.HUD;

public class Panic extends Module {
    public Panic() {
        super(Categories.Misc, "panic", "Turns all modules off");
    }

    @Override
    public void onActivate() {
        Modules.get().disableAll();
        HUD.get().active = false;
    }
}
