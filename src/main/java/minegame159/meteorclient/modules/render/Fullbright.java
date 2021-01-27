/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class Fullbright extends Module {
    public Fullbright() {
        super(Category.Render, "fullbright", "Lights up your world!");
    }

    @Override
    public void onActivate() {
        mc.options.gamma = 16;
    }

    @Override
    public void onDeactivate() {
        mc.options.gamma = 1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.options.gamma = 16;
    }
}
