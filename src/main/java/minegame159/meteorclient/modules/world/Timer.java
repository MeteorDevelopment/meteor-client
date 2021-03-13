/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.world;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Timer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed multiplier.")
            .defaultValue(1)
            .min(0.1)
            .sliderMin(0.1)
            .sliderMax(10)
            .build()
    );

    private double override = -1;

    public Timer() {
        super(Categories.World, "timer", "Changes the speed of everything in your game.");
    }

    public double getMultiplier() {
        if (isActive()) {
            if (override != -1) return override;
            else return speed.get();
        } else {
            return 1;
        }
    }

    public void setOverride(double override) {
        this.override = override;
    }
}
