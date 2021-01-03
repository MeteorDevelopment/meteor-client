/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class CustomFOV extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fov = sgGeneral.add(new IntSetting.Builder()
            .name("fov")
            .description("Your custom FOV.")
            .defaultValue(100)
            .sliderMin(1)
            .sliderMax(179)
            .build()
    );

    private float _fov;

    @Override
    public void onActivate() {
        _fov = (float) mc.options.fov;
        mc.options.fov = fov.get();
    }


    public void getFOV() {
        mc.options.fov = fov.get();
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (fov.get() != mc.options.fov) {
            getFOV();
        }
    });

    @Override
    public void onDeactivate() {
     mc.options.fov = _fov;
    }

    public CustomFOV() {
        super(Category.Render, "custom-fov", "Grants more customizability to your FOV.");
    }

}
