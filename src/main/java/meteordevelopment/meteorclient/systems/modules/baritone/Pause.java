/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

/** One-shot command: press its keybind to pause Baritone's current path (and any running flow). */
public class Pause extends Module {
    public Pause() {
        super(Categories.Baritone, "pause", "Pauses Baritone's current path (and any running flow).");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) PathManagers.get().pause();

        FlowBuilder flowBuilder = Modules.get().get(FlowBuilder.class);
        if (flowBuilder.isRunningFlow()) flowBuilder.pauseRunner();

        toggle();
    }
}
