/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class CustomFOV extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fovSetting = sgGeneral.add(new IntSetting.Builder()
            .name("fov")
            .description("Your custom fov.")
            .defaultValue(100)
            .sliderMin(1)
            .sliderMax(179)
            .build()
    );

    private double fov;

    public CustomFOV() {
        super(Categories.Render, "custom-fov", "Allows your FOV to be more customizable.");
    }

    @Override
    public void onActivate() {
        fov = mc.options.fov;
        mc.options.fov = fovSetting.get();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (fovSetting.get() != mc.options.fov) {
            mc.options.fov = fovSetting.get();
        }
    }

    @Override
    public void onDeactivate() {
     mc.options.fov = fov;
    }
}
