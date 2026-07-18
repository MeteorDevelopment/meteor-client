/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/** One-shot command: press its keybind to resume a path paused by {@link Pause}. */
public class Resume extends Module {
    public Resume() {
        super(Categories.Baritone, "resume", "Resumes Baritone's paused path.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) PathManagers.get().resume();

        toggle();
    }
}
