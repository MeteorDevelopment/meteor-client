/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class FullBright extends ToggleModule {
    private double preGamma;

    public FullBright() {
        super(Category.Render, "full-bright", "No more darkness.");
    }

    @Override
    public void onActivate() {
        preGamma = mc.options.gamma;
        mc.options.gamma = 16;
    }

    @Override
    public void onDeactivate() {
        mc.options.gamma = preGamma;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        mc.options.gamma = 16;
    });
}
