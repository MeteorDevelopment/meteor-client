/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/** One-shot command: press its keybind to cancel Baritone's current path. */
public class Stop extends Module {
    public Stop() {
        super(Categories.Baritone, "stop", "Cancels Baritone's current path.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) PathManagers.get().stop();

        toggle();
    }
}
