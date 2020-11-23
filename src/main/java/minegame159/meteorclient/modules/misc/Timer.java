/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Timer extends ToggleModule {
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

    public Timer() {
        super(Category.Misc, "timer", "Changes speed of everything. WARNING: Lowering it will slow down your game dramatically.");
    }

    public double getMultiplier() {
        return isActive() ? speed.get() : 1;
    }
}
